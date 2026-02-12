package cn.lunadeer.dominion.nms;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.XVersionManager;

import static cn.lunadeer.dominion.utils.Misc.listClassOfPackage;

/**
 * Manages NMS version-specific implementations.
 * <p>
 * Loads the correct implementations based on the current server version at runtime.
 * All NMS-dependent features should access NMS functionality through this singleton.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Initialize (called once during plugin startup, after XVersionManager.VERSION is set)
 * NMSManager.initialize();
 *
 * // Later usage:
 * FakeEntityFactory factory = NMSManager.instance().getFakeEntityFactory();
 * FakeEntity entity = factory.createBlockDisplay(location, blockData);
 * entity.spawn(player);
 * }</pre>
 */
public class NMSManager {

    private static NMSManager INSTANCE;

    private FakeEntityFactory fakeEntityFactory;
    private NMSPacketSender packetSender;

    private NMSManager() {
    }

    /**
     * Initialize the NMS manager by loading version-specific implementations.
     * Must be called after {@link XVersionManager#VERSION} is set.
     *
     * @throws RuntimeException if loading fails
     */
    public static void initialize() {
        INSTANCE = new NMSManager();
        INSTANCE.loadImplementations();
    }

    /**
     * Get the singleton NMSManager instance.
     *
     * @return the NMSManager instance
     * @throws IllegalStateException if not yet initialized
     */
    public static NMSManager instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NMSManager has not been initialized. Call NMSManager.initialize() first.");
        }
        return INSTANCE;
    }

    /**
     * Get the fake entity factory for creating client-side entities.
     */
    public FakeEntityFactory getFakeEntityFactory() {
        return fakeEntityFactory;
    }

    /**
     * Get the packet sender for sending raw NMS packets to players.
     */
    public NMSPacketSender getPacketSender() {
        return packetSender;
    }

    private void loadImplementations() {
        XVersionManager.ImplementationVersion version = XVersionManager.VERSION;
        if (version == null) {
            throw new RuntimeException("Server version not determined. Cannot load NMS implementations.");
        }

        String nmsPackage = "cn.lunadeer.dominion." + version.name() + ".nms";
        while (listClassOfPackage(Dominion.instance, nmsPackage).isEmpty()) {
            version = XVersionManager.VERSION.getPrevious();
            if (version == null) {
                throw new RuntimeException("No compatible NMS implementations found for server version " + XVersionManager.VERSION.name() + " or any previous versions.");
            }
            nmsPackage = "cn.lunadeer.dominion." + version.name() + ".nms";
        }
        nmsPackage += ".";


        XLogger.debug("Loading NMS implementations from package: {0}", nmsPackage);

        try {
            // Load FakeEntityFactory
            Class<?> factoryClass = Class.forName(nmsPackage + "FakeEntityFactoryImpl");
            fakeEntityFactory = (FakeEntityFactory) factoryClass.getDeclaredConstructor().newInstance();
            XLogger.debug("Loaded FakeEntityFactory: {0}", factoryClass.getName());

            // Load NMSPacketSender
            Class<?> senderClass = Class.forName(nmsPackage + "NMSPacketSenderImpl");
            packetSender = (NMSPacketSender) senderClass.getDeclaredConstructor().newInstance();
            XLogger.debug("Loaded NMSPacketSender: {0}", senderClass.getName());

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("NMS implementation not found for version " + version.name() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load NMS implementations for version " + version.name() + ": " + e.getMessage(), e);
        }

        XLogger.info("NMS implementations loaded for version: {0}", version.name());
    }
}
