package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.MemberDOO;
import cn.lunadeer.dominion.doos.TemplateDOO;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.Notification;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionOwner;

public final class TemplateProviderHandler extends TemplateProvider {
    public TemplateProviderHandler() {
        instance = this;
    }

    @Override
    public @NotNull List<TemplateDTO> getTemplates(@NotNull UUID creator) {
        try {
            return List.copyOf(TemplateDOO.selectAll(creator));
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public @Nullable TemplateDTO getTemplate(@NotNull UUID creator, @NotNull String name) {
        try {
            return TemplateDOO.select(creator, name);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable TemplateDTO getTemplate(@NotNull UUID creator, @NotNull Integer id) {
        return getTemplates(creator).stream().filter(template -> template.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public CompletableFuture<TemplateDTO> createTemplate(@NotNull Player operator, @NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateName(name);
                if (TemplateDOO.select(operator.getUniqueId(), name) != null) {
                    throw new DominionException(Language.templateCommandText.templateNameExist, name);
                }
                TemplateDTO result = TemplateDOO.create(operator.getUniqueId(), name);
                Notification.info(operator, Language.templateCommandText.createTemplateSuccess, name);
                return result;
            } catch (Exception e) {
                Notification.error(operator, Language.templateCommandText.createTemplateFail, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<TemplateDTO> renameTemplate(@NotNull Player operator,
                                                          @NotNull TemplateDTO template,
                                                          @NotNull String newName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                assertOwner(operator, template);
                validateName(newName);
                TemplateDTO existing = TemplateDOO.select(operator.getUniqueId(), newName);
                if (existing != null && !existing.getId().equals(template.getId())) {
                    throw new DominionException(Language.templateCommandText.templateNameExist, newName);
                }
                TemplateDTO result = ((TemplateDOO) template).setName(newName);
                Notification.info(operator, Language.templateCommandText.renameTemplateSuccess, newName);
                return result;
            } catch (Exception e) {
                Notification.error(operator, Language.templateCommandText.renameTemplateFail, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<TemplateDTO> deleteTemplate(@NotNull Player operator, @NotNull TemplateDTO template) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                assertOwner(operator, template);
                TemplateDOO.delete(operator.getUniqueId(), template.getName());
                Notification.info(operator, Language.templateCommandText.deleteTemplateSuccess, template.getName());
                return template;
            } catch (Exception e) {
                Notification.error(operator, Language.templateCommandText.deleteTemplateFail, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<TemplateDTO> setTemplateFlag(@NotNull Player operator,
                                                           @NotNull TemplateDTO template,
                                                           @NotNull PriFlag flag,
                                                           boolean value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                assertOwner(operator, template);
                TemplateDTO result = ((TemplateDOO) template).setFlagValue(flag, value);
                Notification.info(operator, Language.templateCommandText.setFlagSuccess,
                        flag.getFlagName(), template.getName(), value);
                return result;
            } catch (Exception e) {
                Notification.error(operator, Language.templateCommandText.setFlagFail, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<MemberDTO> applyTemplate(@NotNull Player operator,
                                                       @NotNull DominionDTO dominion,
                                                       @NotNull MemberDTO member,
                                                       @NotNull TemplateDTO template) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                assertOwner(operator, template);
                if (template.getFlagValue(Flags.ADMIN)) {
                    assertDominionOwner(operator, dominion);
                } else {
                    assertDominionAdmin(operator, dominion);
                }
                ((MemberDOO) member).applyTemplate((TemplateDOO) template);
                Notification.info(operator, Language.templateCommandText.applyTemplateSuccess,
                        template.getName(), member.getPlayer().getLastKnownName());
                return member;
            } catch (Exception e) {
                Notification.error(operator, Language.templateCommandText.applyTemplateFail, e.getMessage());
                return null;
            }
        });
    }

    private static void assertOwner(Player operator, TemplateDTO template) {
        if (!operator.getUniqueId().equals(template.getCreator())) {
            throw new DominionException(Language.templateCommandText.templateNotExist, template.getName());
        }
    }

    private static void validateName(String name) {
        if (name.isBlank() || name.contains(" ")) {
            throw new DominionException(Language.templateCommandText.nameNotValid);
        }
    }
}
