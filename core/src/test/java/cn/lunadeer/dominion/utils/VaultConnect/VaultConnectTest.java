package cn.lunadeer.dominion.utils.VaultConnect;

import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.XLogger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VaultConnectTest {
    private static final Class<net.milkbowl.vault2.economy.Economy> MODERN = net.milkbowl.vault2.economy.Economy.class;
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private JavaPlugin plugin;
    private Plugin vaultPlugin;
    private ServicesManager services;
    private Economy legacy;
    private net.milkbowl.vault2.economy.Economy modern;
    private Player player;
    private VaultConnect connector;
    private VaultConnect previousConnector;
    private XLogger previousLogger;
    private MockedStatic<XLogger> logging;

    enum Protocol { LEGACY, MODERN }
    enum Transaction { WITHDRAW, DEPOSIT }

    @BeforeEach
    void setUp() throws Exception {
        previousConnector = VaultConnect.instance;
        previousLogger = XLogger.instance;
        plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        services = mock(ServicesManager.class);
        vaultPlugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getName()).thenReturn("Dominion");
        when(server.getPluginManager()).thenReturn(plugins);
        when(server.getServicesManager()).thenReturn(services);
        when(plugins.getPlugin("Vault")).thenReturn(vaultPlugin);
        when(vaultPlugin.isEnabled()).thenReturn(true);
        authors(true);
        legacy = mock(Economy.class);
        modern = mock(MODERN);
        when(legacy.isEnabled()).thenReturn(true);
        when(modern.isEnabled()).thenReturn(true);
        when(legacy.currencyNamePlural()).thenReturn("legacy coins");
        when(legacy.currencyNameSingular()).thenReturn("legacy coin");
        when(modern.defaultCurrencyNamePlural("Dominion")).thenReturn("modern coins");
        when(modern.defaultCurrencyNameSingular("Dominion")).thenReturn("modern coin");
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        when(legacy.getBalance(player)).thenReturn(100.0);
        when(modern.getBalance("Dominion", PLAYER_ID)).thenReturn(BigDecimal.valueOf(100));
        // Worker-thread calls in the initialization test use the real disabled logger.
        new XLogger(plugin);
        logging = mockStatic(XLogger.class);
        connector = new VaultConnect(plugin);
    }

    @AfterEach
    void tearDown() {
        if (logging != null) logging.close();
        XLogger.instance = previousLogger;
        VaultConnect.instance = previousConnector;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void registeredModernProviderWinsRegardlessOfVaultAuthors(boolean unlockedAuthor) throws Exception {
        authors(unlockedAuthor);
        registerLegacy();
        registerModern();

        assertEquals("modern coins", connector.currencyNamePlural());
        assertEquals("modern coin", connector.currencyNameSingular());
        verifyNoInteractions(legacy);
        verify(services, never()).getRegistration(Economy.class);
    }

    @Test
    void unlockedWithOnlyLegacyProviderUsesThatProvider() throws Exception {
        registerLegacy();

        assertEquals("legacy coins", connector.currencyNamePlural());
        assertEquals(100, connector.getBalance(player));
        verifyNoInteractions(modern);
    }

    @Test
    void disabledModernProviderFallsBackToEnabledLegacy() throws Exception {
        registerModern();
        registerLegacy();
        when(modern.isEnabled()).thenReturn(false);

        assertEquals("legacy coins", connector.currencyNamePlural());
        verify(modern, never()).defaultCurrencyNamePlural(anyString());
    }

    @Test
    void nullModernProviderFallsBackWithoutPublishingIncompleteAdapter() throws Exception {
        when(services.getRegistration(MODERN)).thenReturn(registration(MODERN, null));
        registerLegacy();

        assertEquals("legacy coins", connector.currencyNamePlural());
        assertEquals(100, connector.getBalance(player));
    }

    @ParameterizedTest
    @MethodSource("initializationFailures")
    void modernInitializationFailureFallsBackToLegacy(Throwable failure) throws Exception {
        registerModern();
        registerLegacy();
        when(modern.isEnabled()).thenThrow(failure);

        assertEquals("legacy coins", connector.currencyNamePlural());
        assertEquals(100, connector.getBalance(player));
        verify(modern, never()).getBalance(anyString(), any(UUID.class));
    }

    @Test
    void noProviderReportsUnavailableAndCanRetryAfterRegistration() throws Exception {
        Exception failure = assertThrows(Exception.class, connector::currencyNamePlural);
        assertEquals(Language.vaultConnectText.economyUnavailable, failure.getMessage());
        registerLegacy();

        assertEquals("legacy coins", connector.currencyNamePlural());
        verify(services, times(2)).getRegistration(MODERN);
        verify(services, times(2)).getRegistration(Economy.class);
    }

    @Test
    void failedInitializationDoesNotCacheThePartiallyInitializedModernAdapter() throws Exception {
        registerModern();
        when(modern.isEnabled()).thenThrow(new NoClassDefFoundError("provider dependency"))
                .thenReturn(true);

        assertThrows(Exception.class, connector::currencyNamePlural);
        assertEquals("modern coins", connector.currencyNamePlural());
        assertEquals(100, connector.getBalance(player));
        verify(services, times(2)).getRegistration(MODERN);
    }

    @Test
    void disabledAndNullProvidersDoNotCountAsAnAvailableEconomy() {
        registerModern();
        when(modern.isEnabled()).thenReturn(false);
        when(services.getRegistration(Economy.class)).thenReturn(registration(Economy.class, null));

        Exception failure = assertThrows(Exception.class, connector::currencyNamePlural);
        assertEquals(Language.vaultConnectText.economyUnavailable, failure.getMessage());
    }

    @Test
    void successfullyInitializedProviderIsReusedWithoutAnotherLookup() throws Exception {
        registerModern();
        registerLegacy();
        assertEquals("modern coins", connector.currencyNamePlural());
        clearInvocations(services);

        assertEquals(100, connector.getBalance(player));
        assertEquals("modern coin", connector.currencyNameSingular());
        verifyNoInteractions(services, legacy);
        verify(modern, times(1)).isEnabled();
    }

    @Test
    void concurrentFirstCallsWaitForInitializationAndUseOneProvider() throws Exception {
        CountDownLatch lookupStarted = new CountDownLatch(1);
        CountDownLatch allowLookup = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        when(services.getRegistration(MODERN)).thenAnswer(invocation -> {
            lookupStarted.countDown();
            if (!allowLookup.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test lookup timed out");
            return registration(MODERN, modern);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> connector.getBalance(player));
            assertTrue(lookupStarted.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return connector.currencyNamePlural();
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS),
                    "A caller must not observe an adapter while its provider is still being initialized");
            allowLookup.countDown();

            assertEquals(100.0, first.get(5, TimeUnit.SECONDS).doubleValue());
            assertEquals("modern coins", second.get(5, TimeUnit.SECONDS));
            verify(services, times(1)).getRegistration(MODERN);
            verify(modern, times(1)).isEnabled();
        } finally {
            allowLookup.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @MethodSource("balanceFailures")
    void connectedProviderBalanceFailurePropagatesWithoutSelectingAnotherProvider(Protocol protocol, Throwable failure) throws Exception {
        connect(protocol);
        if (protocol == Protocol.MODERN) when(modern.getBalance("Dominion", PLAYER_ID)).thenThrow(failure);
        else when(legacy.getBalance(player)).thenThrow(failure);
        clearInvocations(services, legacy, modern);

        assertSame(failure, assertThrows(Throwable.class, () -> connector.getBalance(player)));
        verifyNoInteractions(services);
        verifyNoInteractions(protocol == Protocol.MODERN ? legacy : modern);
    }

    @ParameterizedTest
    @MethodSource("transactionFailures")
    void connectedProviderTransactionExceptionPropagatesWithoutRetryOrFallback(
            Protocol protocol, Transaction transaction, Throwable failure) throws Exception {
        connect(protocol);
        failTransaction(protocol, transaction, failure);
        clearInvocations(services, legacy, modern);

        assertSame(failure, assertThrows(Throwable.class, () -> transact(transaction)));
        verifySingleTransaction(protocol, transaction);
        verifyNoInteractions(services);
        verifyNoInteractions(protocol == Protocol.MODERN ? legacy : modern);
    }

    @ParameterizedTest
    @MethodSource("unsuccessfulResponses")
    void unsuccessfulOrNullTransactionResponseIsAnErrorWithoutRetryOrFallback(
            Protocol protocol, Transaction transaction, boolean nullResponse) throws Exception {
        connect(protocol);
        respond(protocol, transaction, false, nullResponse);
        clearInvocations(services, legacy, modern);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> transact(transaction));
        if (!nullResponse) assertEquals("provider refused transaction", failure.getMessage());
        else assertFalse(failure.getMessage().isBlank());
        verifySingleTransaction(protocol, transaction);
        verifyNoInteractions(services);
        verifyNoInteractions(protocol == Protocol.MODERN ? legacy : modern);
    }

    @ParameterizedTest
    @EnumSource(Protocol.class)
    void successfulResponsesAllowOneWithdrawalAndOneDeposit(Protocol protocol) throws Exception {
        connect(protocol);
        respond(protocol, Transaction.WITHDRAW, true, false);
        respond(protocol, Transaction.DEPOSIT, true, false);
        clearInvocations(services, legacy, modern);

        assertDoesNotThrow(() -> connector.withdrawPlayer(player, 10));
        assertDoesNotThrow(() -> connector.depositPlayer(player, 10));
        verifySingleTransaction(protocol, Transaction.WITHDRAW);
        verifySingleTransaction(protocol, Transaction.DEPOSIT);
        verifyNoInteractions(services);
        verifyNoInteractions(protocol == Protocol.MODERN ? legacy : modern);
    }

    @ParameterizedTest
    @EnumSource(Protocol.class)
    void insufficientFundsNeverCallsProviderWithdrawal(Protocol protocol) throws Exception {
        connect(protocol);
        if (protocol == Protocol.MODERN) when(modern.getBalance("Dominion", PLAYER_ID)).thenReturn(BigDecimal.valueOf(5));
        else when(legacy.getBalance(player)).thenReturn(5.0);
        clearInvocations(services, legacy, modern);

        assertThrows(Exception.class, () -> connector.withdrawPlayer(player, 10));
        verify(legacy, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
        verify(modern, never()).withdraw(anyString(), any(UUID.class), any(BigDecimal.class));
        verifyNoInteractions(services);
        verifyNoInteractions(protocol == Protocol.MODERN ? legacy : modern);
    }

    private void authors(boolean unlocked) throws Exception {
        when(vaultPlugin.getDescription()).thenReturn(new PluginDescriptionFile(new StringReader(
                "name: Vault\nversion: 'test'\nmain: net.milkbowl.vault.Vault\nauthors: ["
                        + (unlocked ? "creatorfromhell" : "cereal") + "]\n")));
    }

    private <T> RegisteredServiceProvider<T> registration(Class<T> api, T provider) {
        return new RegisteredServiceProvider<>(api, provider, ServicePriority.Normal, vaultPlugin);
    }

    private void registerLegacy() { when(services.getRegistration(Economy.class)).thenReturn(registration(Economy.class, legacy)); }
    private void registerModern() { when(services.getRegistration(MODERN)).thenReturn(registration(MODERN, modern)); }

    private void connect(Protocol protocol) throws Exception {
        registerLegacy();
        if (protocol == Protocol.MODERN) registerModern();
        assertEquals(protocol == Protocol.MODERN ? "modern coins" : "legacy coins", connector.currencyNamePlural());
    }

    private void transact(Transaction transaction) throws Exception {
        if (transaction == Transaction.WITHDRAW) connector.withdrawPlayer(player, 10);
        else connector.depositPlayer(player, 10);
    }

    private void failTransaction(Protocol protocol, Transaction transaction, Throwable failure) {
        if (protocol == Protocol.MODERN) {
            if (transaction == Transaction.WITHDRAW) when(modern.withdraw("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0))).thenThrow(failure);
            else when(modern.deposit("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0))).thenThrow(failure);
        } else {
            if (transaction == Transaction.WITHDRAW) when(legacy.withdrawPlayer(player, 10)).thenThrow(failure);
            else when(legacy.depositPlayer(player, 10)).thenThrow(failure);
        }
    }

    private void respond(Protocol protocol, Transaction transaction, boolean success, boolean nullResponse) {
        if (protocol == Protocol.MODERN) {
            var response = nullResponse ? null : new net.milkbowl.vault2.economy.EconomyResponse(
                    BigDecimal.TEN, BigDecimal.valueOf(100), success
                    ? net.milkbowl.vault2.economy.EconomyResponse.ResponseType.SUCCESS
                    : net.milkbowl.vault2.economy.EconomyResponse.ResponseType.FAILURE,
                    success ? "" : "provider refused transaction");
            if (transaction == Transaction.WITHDRAW) when(modern.withdraw("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0))).thenReturn(response);
            else when(modern.deposit("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0))).thenReturn(response);
        } else {
            var response = nullResponse ? null : new EconomyResponse(10, 100, success
                    ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                    success ? "" : "provider refused transaction");
            if (transaction == Transaction.WITHDRAW) when(legacy.withdrawPlayer(player, 10)).thenReturn(response);
            else when(legacy.depositPlayer(player, 10)).thenReturn(response);
        }
    }

    private void verifySingleTransaction(Protocol protocol, Transaction transaction) {
        if (protocol == Protocol.MODERN) {
            if (transaction == Transaction.WITHDRAW) verify(modern, times(1)).withdraw("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0));
            else verify(modern, times(1)).deposit("Dominion", PLAYER_ID, BigDecimal.valueOf(10.0));
        } else {
            if (transaction == Transaction.WITHDRAW) verify(legacy, times(1)).withdrawPlayer(player, 10);
            else verify(legacy, times(1)).depositPlayer(player, 10);
        }
    }

    private static Stream<Throwable> initializationFailures() {
        return Stream.of(new IllegalStateException("provider initialization failed"),
                new NoClassDefFoundError("provider dependency missing"), new AbstractMethodError("provider API mismatch"));
    }

    private static Stream<Arguments> balanceFailures() {
        return Stream.of(Protocol.values()).flatMap(protocol -> initializationFailures().map(error -> Arguments.of(protocol, error)));
    }

    private static Stream<Arguments> transactionFailures() {
        return Stream.of(Protocol.values()).flatMap(protocol -> Stream.of(Transaction.values())
                .flatMap(transaction -> initializationFailures().map(error -> Arguments.of(protocol, transaction, error))));
    }

    private static Stream<Arguments> unsuccessfulResponses() {
        return Stream.of(Protocol.values()).flatMap(protocol -> Stream.of(Transaction.values())
                .flatMap(transaction -> Stream.of(false, true).map(nullResponse -> Arguments.of(protocol, transaction, nullResponse))));
    }
}
