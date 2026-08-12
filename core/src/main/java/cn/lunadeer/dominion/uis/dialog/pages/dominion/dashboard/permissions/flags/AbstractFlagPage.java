package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.flags;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Shared domain operations for the flag-group and flag-list pages. */
abstract class AbstractFlagPage extends AbstractDialogPage {
    protected record PendingFlagChange(Flag flag, boolean target, long version) {
    }

    protected AbstractFlagPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    protected String flagTitle(DialogMenuId context, DominionDTO dominion, TemplateDTO template) {
        return switch (context) {
            case ENV_FLAGS -> configured("titles.environment-flags",
                    Map.of("dominion", dominion.getName()));
            case GUEST_FLAGS -> configured("titles.guest-flags",
                    Map.of("dominion", dominion.getName()));
            case MEMBER_FLAGS -> config.text("titles.member-flags");
            case GROUP_FLAGS -> config.text("titles.group-flags");
            case TEMPLATE_FLAGS -> configured("titles.template-flags",
                    Map.of("template", template.getName()));
            default -> config.text("titles.flags");
        };
    }

    protected Component flagActionLabel(Flag flag, boolean enabled) {
        return Component.text(flag.getDisplayName(), NamedTextColor.WHITE)
                .append(Component.text(enabled ? " ✔" : " ❌",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    protected boolean effectiveFlagState(DialogMenuSession session, String prefix,
                                         DialogMenuId context, DialogRoute route,
                                         DominionDTO dominion, TemplateDTO template,
                                         Flag flag) {
        Boolean draft = session.state(flagDraftKey(prefix, flag), Boolean.class);
        return draft == null ? flagState(context, route, dominion, template, flag) : draft;
    }

    protected PendingFlagChange markFlagDraft(DialogMenuSession session, String prefix,
                                              Flag flag, boolean target) {
        String versionKey = flagDraftVersionKey(prefix, flag);
        Long previous = session.state(versionKey, Long.class);
        long version = previous == null ? 1L : previous + 1L;
        session.state(flagDraftKey(prefix, flag), target);
        session.state(versionKey, version);
        return new PendingFlagChange(flag, target, version);
    }

    protected void toggleFlag(Player player, DialogMenuSession session, String draftPrefix,
                              DialogRoute route, DialogMenuId context,
                              DominionDTO dominion, TemplateDTO template, Flag flag) {
        boolean target = !effectiveFlagState(session, draftPrefix, context, route,
                dominion, template, flag);
        PendingFlagChange change = markFlagDraft(session, draftPrefix, flag, target);
        nav.refresh(player);
        applyFlagChangesAsync(player, session, draftPrefix, route, context,
                dominion, template, List.of(change));
    }

    protected void toggleFlagGroup(Player player, DialogMenuSession session, String draftPrefix,
                                   DialogRoute route, DialogMenuId context,
                                   DominionDTO dominion, TemplateDTO template,
                                   List<? extends Flag> flags) {
        boolean target = FlagGroupActions.bulkTarget(flags,
                flag -> effectiveFlagState(session, draftPrefix, context, route,
                        dominion, template, flag));
        List<PendingFlagChange> changes = flags.stream()
                .map(flag -> markFlagDraft(session, draftPrefix, flag, target)).toList();
        nav.refresh(player);
        applyFlagChangesAsync(player, session, draftPrefix, route, context,
                dominion, template, changes);
    }

    private void applyFlagChangesAsync(Player player, DialogMenuSession session,
                                       String draftPrefix, DialogRoute route,
                                       DialogMenuId context, DominionDTO dominion,
                                       TemplateDTO template,
                                       List<PendingFlagChange> changes) {
        changes.forEach(change -> startFlagChange(player, session, draftPrefix, route,
                context, dominion, template, change));
    }

    private void startFlagChange(Player player, DialogMenuSession session, String draftPrefix,
                                 DialogRoute route, DialogMenuId context,
                                 DominionDTO dominion, TemplateDTO template,
                                 PendingFlagChange change) {
        String inFlightKey = flagDraftInFlightKey(draftPrefix, change.flag());
        if (Boolean.TRUE.equals(session.state(inFlightKey, Boolean.class))) return;
        session.state(inFlightKey, true);
        scheduleFlagChange(player, route, context, dominion, template, change)
                .whenComplete((ignored, throwable) -> Scheduler.runEntityTask(() -> {
                    session.removeState(inFlightKey);
                    PendingFlagChange latest = currentFlagDraft(session, draftPrefix, change.flag());
                    boolean isLatest = latest != null && latest.version() == change.version();
                    if (throwable != null) {
                        if (isLatest) clearFlagDraft(session, draftPrefix, change.flag());
                        Notification.error(player, throwable);
                    } else if (isLatest) {
                        clearFlagDraft(session, draftPrefix, change.flag());
                    }
                    if (player.isOnline()) nav.refresh(player);
                    PendingFlagChange next = currentFlagDraft(session, draftPrefix, change.flag());
                    if (next != null) {
                        startFlagChange(player, session, draftPrefix, route, context,
                                dominion, template, next);
                    }
                }, player));
    }

    private CompletableFuture<?> scheduleFlagChange(Player player, DialogRoute route,
                                                    DialogMenuId context, DominionDTO dominion,
                                                    TemplateDTO template, PendingFlagChange change) {
        CompletableFuture<Object> scheduled = new CompletableFuture<>();
        try {
            Scheduler.runEntityTask(() -> {
                try {
                    CompletableFuture<?> update = setFlag(player, route, context, dominion,
                            template, change.flag(), change.target());
                    if (update == null) {
                        scheduled.complete(null);
                    } else {
                        update.whenComplete((ignored, throwable) -> {
                            if (throwable == null) scheduled.complete(null);
                            else scheduled.completeExceptionally(throwable);
                        });
                    }
                } catch (Throwable throwable) {
                    scheduled.completeExceptionally(throwable);
                }
            }, player);
        } catch (Throwable throwable) {
            scheduled.completeExceptionally(throwable);
        }
        return scheduled;
    }

    private PendingFlagChange currentFlagDraft(DialogMenuSession session, String prefix, Flag flag) {
        Long version = session.state(flagDraftVersionKey(prefix, flag), Long.class);
        Boolean target = session.state(flagDraftKey(prefix, flag), Boolean.class);
        return version == null || target == null ? null : new PendingFlagChange(flag, target, version);
    }

    private void clearFlagDraft(DialogMenuSession session, String prefix, Flag flag) {
        session.removeState(flagDraftKey(prefix, flag));
        session.removeState(flagDraftVersionKey(prefix, flag));
    }

    private CompletableFuture<?> setFlag(Player player, DialogRoute route, DialogMenuId context,
                                         DominionDTO dominion, TemplateDTO template,
                                         Flag flag, boolean value) {
        return switch (context) {
            case ENV_FLAGS -> DominionProvider.getInstance()
                    .setDominionEnvFlag(player, dominion, (EnvFlag) flag, value);
            case GUEST_FLAGS -> DominionProvider.getInstance()
                    .setDominionGuestFlag(player, dominion, (PriFlag) flag, value);
            case MEMBER_FLAGS -> MemberProvider.getInstance().setMemberFlag(
                    player, dominion, requireMember(dominion, route.integer("member")),
                    (PriFlag) flag, value);
            case GROUP_FLAGS -> GroupProvider.getInstance().setGroupFlag(
                    player, dominion, requireGroup(dominion, route.integer("group")),
                    (PriFlag) flag, value);
            case TEMPLATE_FLAGS -> TemplateProvider.getInstance()
                    .setTemplateFlag(player, template, (PriFlag) flag, value);
            default -> CompletableFuture.completedFuture(null);
        };
    }

    protected boolean flagState(DialogMenuId context, DialogRoute route, DominionDTO dominion,
                                TemplateDTO template, Flag flag) {
        return switch (context) {
            case ENV_FLAGS -> dominion.getEnvFlagValue((EnvFlag) flag);
            case GUEST_FLAGS -> dominion.getGuestFlagValue((PriFlag) flag);
            case MEMBER_FLAGS -> requireMember(dominion, route.integer("member"))
                    .getFlagValue((PriFlag) flag);
            case GROUP_FLAGS -> requireGroup(dominion, route.integer("group"))
                    .getFlagValue((PriFlag) flag);
            case TEMPLATE_FLAGS -> template.getFlagValue((PriFlag) flag);
            default -> false;
        };
    }

    protected FlagGroup<?> requireFlagGroup(DialogRoute route, DialogMenuId context) {
        String groupId = route.string("flag-group");
        FlagGroup<?> group;
        if (context == DialogMenuId.ENV_FLAGS) {
            group = groupId.equals("ungrouped")
                    ? FlagGroups.getUngroupedEnvFlags() : FlagGroups.getEnvFlagGroup(groupId);
        } else {
            group = groupId.equals("ungrouped")
                    ? FlagGroups.getUngroupedPriFlags() : FlagGroups.getPriFlagGroup(groupId);
        }
        if (group == null) throw new IllegalStateException("Flag group no longer exists: " + groupId);
        return group;
    }

    protected DialogRoute groupRoute(DialogRoute source, DialogMenuId context, String groupId) {
        return new DialogRoute(DialogMenuId.FLAG_LIST.name(), source.parameters(), 1, "")
                .with("context", context.name()).with("flag-group", groupId);
    }

    protected DialogMenuId flagContext(DialogRoute route) {
        return id(route) == DialogMenuId.FLAG_LIST
                ? DialogMenuId.valueOf(route.string("context")) : id(route);
    }

    protected static String flagDraftKey(String prefix, Flag flag) {
        return prefix + flag.getFlagName();
    }

    private static String flagDraftVersionKey(String prefix, Flag flag) {
        return prefix + "version:" + flag.getFlagName();
    }

    private static String flagDraftInFlightKey(String prefix, Flag flag) {
        return prefix + "in-flight:" + flag.getFlagName();
    }

    private static String flagDraftPrefix(DialogRoute route, DialogMenuId context, String groupId) {
        return "flag-draft:" + context + ':' + route.parameters() + ':' + groupId + ':';
    }

    protected String draftPrefix(DialogRoute route, DialogMenuId context, String groupId) {
        return flagDraftPrefix(route, context, groupId);
    }
}
