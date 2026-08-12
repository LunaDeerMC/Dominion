package cn.lunadeer.dominion.utils.dialogui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Complete, version-independent description of a Minecraft dialog.
 *
 * <p>The element interfaces are intentionally open. Version backends register
 * encoders by {@link Element#kind()} so future protocol elements do not require
 * changing the NMS bridge contract.</p>
 */
public record DialogSpec(Base base, Type type) {
    public static final String RESERVED_INPUT_PREFIX = "__dominion_";

    public DialogSpec {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(type, "type");
    }

    public static Builder builder(Component title, Type type) {
        return new Builder(title, type);
    }

    public interface Element {
        DialogKey kind();
    }

    public enum AfterAction {
        CLOSE,
        NONE,
        WAIT_FOR_RESPONSE
    }

    public record Base(
            Component title,
            Component externalTitle,
            boolean canCloseWithEscape,
            boolean pause,
            AfterAction afterAction,
            List<Body> body,
            List<Input> inputs
    ) {
        public Base {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(afterAction, "afterAction");
            body = List.copyOf(body);
            inputs = List.copyOf(inputs);
        }
    }

    public static final class Builder {
        private final Component title;
        private Component externalTitle;
        private boolean canCloseWithEscape = true;
        private boolean pause = false;
        private AfterAction afterAction = AfterAction.CLOSE;
        private final List<Body> body = new ArrayList<>();
        private final List<Input> inputs = new ArrayList<>();
        private Type type;

        private Builder(Component title, Type type) {
            this.title = Objects.requireNonNull(title, "title");
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder externalTitle(Component value) {
            externalTitle = value;
            return this;
        }

        public Builder canCloseWithEscape(boolean value) {
            canCloseWithEscape = value;
            return this;
        }

        public Builder pause(boolean value) {
            pause = value;
            return this;
        }

        public Builder afterAction(AfterAction value) {
            afterAction = Objects.requireNonNull(value, "afterAction");
            return this;
        }

        public Builder body(Body value) {
            body.add(Objects.requireNonNull(value, "body"));
            return this;
        }

        public Builder bodies(List<? extends Body> values) {
            body.addAll(values);
            return this;
        }

        public Builder input(Input value) {
            inputs.add(Objects.requireNonNull(value, "input"));
            return this;
        }

        public Builder inputs(List<? extends Input> values) {
            inputs.addAll(values);
            return this;
        }

        public Builder type(Type value) {
            type = Objects.requireNonNull(value, "type");
            return this;
        }

        public DialogSpec build() {
            DialogSpec spec = new DialogSpec(
                    new Base(title, externalTitle, canCloseWithEscape, pause, afterAction, body, inputs),
                    type
            );
            DialogModelValidator.validate(spec);
            return spec;
        }
    }

    public interface Body extends Element {
    }

    public record PlainMessageBody(Component contents, int width) implements Body {
        public static final DialogKey KIND = DialogKey.of("minecraft", "plain_message");

        public PlainMessageBody {
            Objects.requireNonNull(contents, "contents");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record ItemBody(
            ItemStack item,
            PlainMessageBody description,
            boolean showDecorations,
            boolean showTooltip,
            int width,
            int height
    ) implements Body {
        public static final DialogKey KIND = DialogKey.of("minecraft", "item");

        public ItemBody {
            Objects.requireNonNull(item, "item");
            item = item.clone();
        }

        @Override
        public ItemStack item() {
            return item.clone();
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public interface Input extends Element {
        String key();
    }

    public record Multiline(Integer maxLines, Integer height) {
    }

    public record TextInput(
            String key,
            int width,
            Component label,
            boolean labelVisible,
            String initial,
            int maxLength,
            Multiline multiline
    ) implements Input {
        public static final DialogKey KIND = DialogKey.of("minecraft", "text");

        public TextInput {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(initial, "initial");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record BooleanInput(
            String key,
            Component label,
            boolean initial,
            String onTrue,
            String onFalse
    ) implements Input {
        public static final DialogKey KIND = DialogKey.of("minecraft", "boolean");

        public BooleanInput {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(onTrue, "onTrue");
            Objects.requireNonNull(onFalse, "onFalse");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record NumberRangeInput(
            String key,
            int width,
            Component label,
            String labelFormat,
            float start,
            float end,
            Float initial,
            Float step
    ) implements Input {
        public static final DialogKey KIND = DialogKey.of("minecraft", "number_range");

        public NumberRangeInput {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(labelFormat, "labelFormat");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record Option(String id, Component display, boolean initial) {
        public Option {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Option id cannot be blank");
        }
    }

    public record SingleOptionInput(
            String key,
            int width,
            List<Option> options,
            Component label,
            boolean labelVisible
    ) implements Input {
        public static final DialogKey KIND = DialogKey.of("minecraft", "single_option");

        public SingleOptionInput {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            options = List.copyOf(options);
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public interface Type extends Element {
    }

    public record Notice(ActionButton action) implements Type {
        public static final DialogKey KIND = DialogKey.of("minecraft", "notice");

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record Confirmation(ActionButton yes, ActionButton no) implements Type {
        public static final DialogKey KIND = DialogKey.of("minecraft", "confirmation");

        public Confirmation {
            Objects.requireNonNull(yes, "yes");
            Objects.requireNonNull(no, "no");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record MultiAction(List<ActionButton> actions, ActionButton exitAction, int columns) implements Type {
        public static final DialogKey KIND = DialogKey.of("minecraft", "multi_action");

        public MultiAction {
            actions = List.copyOf(actions);
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public interface Reference {
    }

    public record InlineReference(DialogSpec dialog) implements Reference {
        public InlineReference {
            Objects.requireNonNull(dialog, "dialog");
        }
    }

    public record KeyedReference(DialogKey key) implements Reference {
        public KeyedReference {
            Objects.requireNonNull(key, "key");
        }
    }

    public record DialogList(
            List<Reference> dialogs,
            ActionButton exitAction,
            int columns,
            int buttonWidth
    ) implements Type {
        public static final DialogKey KIND = DialogKey.of("minecraft", "dialog_list");

        public DialogList {
            dialogs = List.copyOf(dialogs);
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record ServerLinks(ActionButton exitAction, int columns, int buttonWidth) implements Type {
        public static final DialogKey KIND = DialogKey.of("minecraft", "server_links");

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    /**
     * Version-independent data for Minecraft's native player-head text object.
     *
     * <p>A valid stored texture URL renders the player's skin without a profile
     * lookup. Invalid profile names and unavailable or malformed texture URLs
     * are normalized to Minecraft's built-in Steve skin.</p>
     */
    public record PlayerHeadIcon(
            UUID playerId,
            String playerName,
            String skinTextureUrl,
            boolean hat
    ) {
        public static final String DEFAULT_PLAYER_NAME = "Steve";
        public static final String DEFAULT_SKIN_TEXTURE = "minecraft:entity/player/wide/steve";

        public PlayerHeadIcon {
            Objects.requireNonNull(playerId, "playerId");
            String normalizedName = playerName == null ? null : playerName.trim();
            if (!isValidPlayerName(normalizedName)) {
                playerName = DEFAULT_PLAYER_NAME;
                skinTextureUrl = null;
            } else {
                playerName = normalizedName;
                skinTextureUrl = validSkinTextureUrl(skinTextureUrl);
            }
        }

        public PlayerHeadIcon(UUID playerId, String playerName, String skinTextureUrl) {
            this(playerId, playerName, skinTextureUrl, true);
        }

        public boolean usesDefaultSkin() {
            return skinTextureUrl == null;
        }

        private static boolean isValidPlayerName(String value) {
            return value != null && !value.isEmpty() && value.length() <= 16
                    && value.chars().allMatch(character -> character > 32 && character < 127);
        }

        private static String validSkinTextureUrl(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                String normalized = value.trim();
                URI texture = URI.create(normalized);
                String scheme = texture.getScheme();
                if (scheme == null || texture.getHost() == null
                        || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                    return null;
                }
                return normalized;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    /**
     * A button in a dialog.
     *
     * <p>{@code icon} is an atlas resource path in the form
     * {@code namespace:atlas/sprite}, for example
     * {@code minecraft:items/item/emerald}. When {@code playerHead} is set,
     * 1.21.9+ backends render it instead and retain the atlas icon as a
     * fallback for backends that do not support native player-head objects.</p>
     */
    public record ActionButton(
            Component label,
            Component tooltip,
            int width,
            String icon,
            PlayerHeadIcon playerHead,
            Action action
    ) {
        public ActionButton {
            Objects.requireNonNull(label, "label");
        }

        public ActionButton(Component label, Component tooltip, int width, Action action) {
            this(label, tooltip, width, null, null, action);
        }

        public ActionButton(Component label, Component tooltip, int width, String icon, Action action) {
            this(label, tooltip, width, icon, null, action);
        }

        public static ActionButton of(Component label, Action action) {
            return new ActionButton(label, null, 150, null, null, action);
        }

        public static ActionButton of(Component label, String icon, Action action) {
            return new ActionButton(label, null, 150, icon, null, action);
        }

        public static ActionButton of(Component label, int width, String icon, Action action) {
            return new ActionButton(label, null, width, icon, null, action);
        }

        public static ActionButton of(Component label, int width, String fallbackIcon,
                                      PlayerHeadIcon playerHead, Action action) {
            return new ActionButton(label, null, width, fallbackIcon, playerHead, action);
        }
    }

    public interface Action extends Element {
    }

    @FunctionalInterface
    public interface Callback {
        void handle(Player player, DialogResponse response);
    }

    public record CallbackOptions(Duration lifetime, int uses) {
        public static final int UNLIMITED_USES = -1;
        public static final CallbackOptions DEFAULT = new CallbackOptions(Duration.ofMinutes(2), 1);

        public CallbackOptions {
            Objects.requireNonNull(lifetime, "lifetime");
            if (lifetime.isZero() || lifetime.isNegative()) {
                throw new IllegalArgumentException("Callback lifetime must be positive");
            }
            if (uses != UNLIMITED_USES && uses < 1) {
                throw new IllegalArgumentException("Callback uses must be positive or unlimited");
            }
        }
    }

    public record CallbackAction(Callback callback, CallbackOptions options) implements Action {
        public static final DialogKey KIND = DialogKey.of("dominion", "callback");

        public CallbackAction {
            Objects.requireNonNull(callback, "callback");
            options = options == null ? CallbackOptions.DEFAULT : options;
        }

        public CallbackAction(Callback callback) {
            this(callback, CallbackOptions.DEFAULT);
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record CustomClickAction(DialogKey id, DialogPayload additions) implements Action {
        public static final DialogKey KIND = DialogKey.of("minecraft", "dynamic/custom");

        public CustomClickAction {
            Objects.requireNonNull(id, "id");
            additions = additions == null ? DialogPayload.EMPTY : additions;
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public record CommandTemplateAction(String template) implements Action {
        public static final DialogKey KIND = DialogKey.of("minecraft", "dynamic/run_command");

        public CommandTemplateAction {
            if (template == null || template.isBlank()) {
                throw new IllegalArgumentException("Command template cannot be blank");
            }
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }

    public interface StaticClick {
    }

    public record OpenUrl(URI uri) implements StaticClick {
        public OpenUrl {
            Objects.requireNonNull(uri, "uri");
        }
    }

    public record RunCommand(String command) implements StaticClick {
        public RunCommand {
            if (command == null || command.isBlank()) throw new IllegalArgumentException("Command cannot be blank");
        }
    }

    public record SuggestCommand(String command) implements StaticClick {
        public SuggestCommand {
            Objects.requireNonNull(command, "command");
        }
    }

    public record ShowDialog(Reference dialog) implements StaticClick {
        public ShowDialog {
            Objects.requireNonNull(dialog, "dialog");
        }
    }

    public record ChangePage(int page) implements StaticClick {
    }

    public record CopyToClipboard(String value) implements StaticClick {
        public CopyToClipboard {
            Objects.requireNonNull(value, "value");
        }
    }

    public record StaticCustomClick(DialogKey id, DialogPayload payload) implements StaticClick {
        public StaticCustomClick {
            Objects.requireNonNull(id, "id");
            payload = payload == null ? DialogPayload.EMPTY : payload;
        }
    }

    public record StaticAction(StaticClick value) implements Action {
        public static final DialogKey KIND = DialogKey.of("minecraft", "static");

        public StaticAction {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public DialogKey kind() {
            return KIND;
        }
    }
}
