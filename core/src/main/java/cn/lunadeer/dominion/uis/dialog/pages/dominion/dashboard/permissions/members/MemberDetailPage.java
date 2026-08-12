package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.members;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** Edits one member's flags, template, group assignment, or membership. */
public final class MemberDetailPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public MemberDetailPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        MemberDTO member = requireMember(dominion, route.integer("member"));
        Map<String, Object> values = memberValues(dominion, member);
        DominionDialogPage page = new DominionDialogPage(config, "member-detail", values, LAYOUT)
                .summary("info", values)
                .action("flags", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.MEMBER_FLAGS, dominion)
                                        .with("member", member.getId())))
                .action("template", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.TEMPLATE_PICKER, dominion)
                                        .with("member", member.getId())));
        boolean assigned = member.getGroupId() != -1;
        page.action(DominionDialogPage.component(configured(
                                assigned ? "labels.remove-from-group" : "labels.assign-to-group", values)),
                        DominionDialogPage.component(configured(
                                assigned ? "descriptions.remove-from-group"
                                        : "descriptions.assign-to-group", values)),
                        LAYOUT.buttonWidth(), page.icon(assigned ? "remove-group" : "assign-group"),
                        (viewer, response) -> updateMemberGroup(viewer, dominion, member))
                .action("remove", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.remove-member", Map.of(
                                        "player", member.getPlayer().getLastKnownName())),
                                confirmed -> ui.submit(viewer,
                                        MemberProvider.getInstance().removeMember(viewer, dominion, member),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build();
    }

    private void updateMemberGroup(Player player, DominionDTO dominion, MemberDTO member) {
        if (member.getGroupId() == -1) {
            nav.push(player, route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                    .with("member", member.getId()).with("mode", "assign"));
            return;
        }
        GroupDTO group = api.getGroup(member.getGroupId());
        if (group != null) {
            ui.submit(player, GroupProvider.getInstance().removeMember(player, dominion, group, member),
                    ignored -> {});
        }
    }
}
