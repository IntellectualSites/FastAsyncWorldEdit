package com.boydti.fawe.bukkit.chat;

import static com.boydti.fawe.bukkit.chat.TextualComponent.rawText;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a formattable message. Such messages can use elements such as colors, formatting
 * codes, hover and click data, and other features provided by the vanilla Minecraft <a
 * href="http://minecraft.gamepedia.com/Tellraw#Raw_JSON_Text">JSON message formatter</a>. This
 * class allows plugins to emulate the functionality of the vanilla Minecraft <a
 * href="http://minecraft.gamepedia.com/Commands#tellraw">tellraw command</a>.
 * <p>
 * This class follows the builder pattern, allowing for method chaining. It is set up such that
 * invocations of property-setting methods will affect the current editing component, and a call to
 * {@link #then()} or {@link #then(String)} will append a new editing component to the end of the
 * message, optionally initializing it with text. Further property-setting method calls will affect
 * that editing component.
 * </p>
 */
public class FancyMessage implements JsonRepresentedObject, Cloneable, Iterable<MessagePart>,
    ConfigurationSerializable {

    static {
        ConfigurationSerialization.registerClass(FancyMessage.class);
    }

    private List<MessagePart> messageParts;
    private int index = 0;
    private String jsonString;
    private boolean dirty;

    private static Constructor<?> nmsPacketPlayOutChatConstructor;

    @Override
    public FancyMessage clone() throws CloneNotSupportedException {
        FancyMessage instance = (FancyMessage) super.clone();
        instance.messageParts = new ArrayList<>(messageParts.size());
        for (int i = 0; i < messageParts.size(); i++) {
            instance.messageParts.add(i, messageParts.get(i).clone());
        }
        instance.index = index;
        instance.dirty = false;
        instance.jsonString = null;
        return instance;
    }

    /**
     * Creates a JSON message with text.
     *
     * @param firstPartText The existing text in the message.
     */
    public FancyMessage(final String firstPartText) {
        this(rawText(firstPartText));
    }

    private FancyMessage(final TextualComponent firstPartText) {
        messageParts = new ArrayList<>();
        messageParts.add(new MessagePart(firstPartText));
        index = messageParts.size();
        jsonString = null;
        dirty = false;
        if (nmsPacketPlayOutChatConstructor == null) {
            try {
                nmsPacketPlayOutChatConstructor = Reflection.getNMSClass("PacketPlayOutChat")
                    .getDeclaredConstructor(Reflection.getNMSClass("IChatBaseComponent"));
                nmsPacketPlayOutChatConstructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Bukkit.getLogger()
                    .log(Level.SEVERE, "Could not find Minecraft method or constructor.", e);
            } catch (SecurityException e) {
                Bukkit.getLogger().log(Level.WARNING, "Could not access constructor.", e);
            }
        }
    }

    /**
     * Creates a JSON message without text.
     */
    public FancyMessage() {
        this((TextualComponent) null);
    }

    /**
     * Sets the text of the current editing component to a value.
     *
     * @param text The new text of the current editing component.
     * @return This builder instance.
     */
    public FancyMessage text(String text) {
        MessagePart latest = latest();
        latest.text = rawText(text);
        dirty = true;
        return this;
    }

    /**
     * Sets the text of the current editing component to a value.
     *
     * @param text The new text of the current editing component.
     * @return This builder instance.
     */
    public FancyMessage text(TextualComponent text) {
        MessagePart latest = latest();
        latest.text = text;
        dirty = true;
        return this;
    }

    /**
     * @param text Text with coloring
     * @return This builder instance.
     * @throws IllegalArgumentException If the specified {@code ChatColor} enumeration value is not
     *                                  a color (but a format value).
     */
    public FancyMessage color(String text) {
        index = messageParts.size();
        boolean color = false;
        ArrayDeque<ChatColor> colors = new ArrayDeque<>();
        int last = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (color != (color = false)) {
                ChatColor chatColor = ChatColor.getByChar(c);
                if (chatColor != null) {
                    if (i - last > 1) {
                        append(text.substring(last, i - 1));
                        colors.forEach(this::color);
                        colors.clear();
                    }
                    colors.add(chatColor);
                    last = i + 1;
                }
            }
            if (c == '\u00A7') {
                color = true;
            }
        }
        if (text.length() - last > 0) {
            append(text.substring(last, text.length()));
            colors.forEach(this::color);
        }
        index++;
        return this;
    }

    /**
     * Sets the color of the current editing component to a value.<br /> Setting the color will
     * clear current styles
     *
     * @param color The new color of the current editing component.
     * @return This builder instance.
     * @throws IllegalArgumentException If the specified {@code ChatColor} enumeration value is not
     *                                  a color (but a format value).
     */
    public FancyMessage color(ChatColor color) {
        if (!color.isColor()) {
            if (color.isFormat()) {
                return style(color);
            }
            if (color == ChatColor.RESET) {
                color = ChatColor.WHITE;
            }
        } else {
            latest().styles.clear();
        }
        latest().color = color;
        dirty = true;
        return this;
    }

    /**
     * Sets the stylization of the current editing component.
     *
     * @param styles The array of styles to apply to the editing component.
     * @return This builder instance.
     * @throws IllegalArgumentException If any of the enumeration values in the array do not
     *                                  represent formatters.
     */
    public FancyMessage style(ChatColor... styles) {
        for (final ChatColor style : styles) {
            if (!style.isFormat()) {
                color(style);
            }
        }
        latest().styles.addAll(Arrays.asList(styles));
        dirty = true;
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to open a file on
     * the client side filesystem when the currently edited part of the {@code FancyMessage} is
     * clicked.
     *
     * @param path The path of the file on the client filesystem.
     * @return This builder instance.
     */
    public FancyMessage file(final String path) {
        onClick("open_file", path);
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to open a webpage in
     * the client's web browser when the currently edited part of the {@code FancyMessage} is
     * clicked.
     *
     * @param url The URL of the page to open when the link is clicked.
     * @return This builder instance.
     */
    public FancyMessage link(final String url) {
        onClick("open_url", url);
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to replace the chat
     * input box content with the specified string when the currently edited part of the {@code
     * FancyMessage} is clicked. The client will not immediately send the command to the server to
     * be executed unless the client player submits the command/chat message, usually with the enter
     * key.
     *
     * @param command The text to display in the chat bar of the client.
     * @return This builder instance.
     */
    public FancyMessage suggest(final String command) {
        onClick("suggest_command", command);
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to append the chat
     * input box content with the specified string when the currently edited part of the {@code
     * FancyMessage} is SHIFT-CLICKED. The client will not immediately send the command to the
     * server to be executed unless the client player submits the command/chat message, usually with
     * the enter key.
     *
     * @param command The text to append to the chat bar of the client.
     * @return This builder instance.
     */
    public FancyMessage insert(final String command) {
        onCurrent(m -> m.insertionData = command);
        dirty = true;
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to send the
     * specified string to the server as a chat message when the currently edited part of the {@code
     * FancyMessage} is clicked. The client <b>will</b> immediately send the command to the server
     * to be executed when the editing component is clicked.
     *
     * @param command The text to display in the chat bar of the client.
     * @return This builder instance.
     */
    public FancyMessage command(final String command) {
        onClick("run_command", command);
        return this;
    }

    /**
     * Set the behavior of the current editing component to display raw text when the client hovers
     * over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param text The text, which supports newlines, which will be displayed to the client upon
     *             hovering.
     * @return This builder instance.
     */
    public FancyMessage tooltip(final String text) {
        onHover("show_text", new JsonString(text));
        return this;
    }

    /**
     * Set the behavior of the current editing component to display raw text when the client hovers
     * over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param lines The lines of text which will be displayed to the client upon hovering. The
     *              iteration order of this object will be the order in which the lines of the
     *              tooltip are created.
     * @return This builder instance.
     */
    public FancyMessage tooltip(final Iterable<String> lines) {
        tooltip(com.boydti.fawe.bukkit.chat.ArrayWrapper.toArray(lines, String.class));
        return this;
    }

    /**
     * Set the behavior of the current editing component to display raw text when the client hovers
     * over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param lines The lines of text which will be displayed to the client upon hovering.
     * @return This builder instance.
     */
    public FancyMessage tooltip(final String... lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(lines[i]);
            if (i != lines.length - 1) {
                builder.append('\n');
            }
        }
        tooltip(builder.toString());
        return this;
    }

    /**
     * Set the behavior of the current editing component to display formatted text when the client
     * hovers over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param text The formatted text which will be displayed to the client upon hovering.
     * @return This builder instance.
     */
    public FancyMessage formattedTooltip(FancyMessage text) {
        for (MessagePart component : text.messageParts) {
            if (component.clickActionData != null && component.clickActionName != null) {
                throw new IllegalArgumentException("The tooltip text cannot have click data.");
            } else if (component.hoverActionData != null && component.hoverActionName != null) {
                throw new IllegalArgumentException("The tooltip text cannot have a tooltip.");
            }
        }
        onHover("show_text", text);
        return this;
    }

    /**
     * Set the behavior of the current editing component to display the specified lines of formatted
     * text when the client hovers over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param lines The lines of formatted text which will be displayed to the client upon
     *              hovering.
     * @return This builder instance.
     */
    public FancyMessage formattedTooltip(FancyMessage... lines) {
        if (lines.length < 1) {
            onHover(null, null); // Clear tooltip
            return this;
        }

        FancyMessage result = new FancyMessage();
        result.messageParts
            .clear(); // Remove the one existing text component that exists by default, which destabilizes the object

        for (int i = 0; i < lines.length; i++) {
            try {
                for (MessagePart component : lines[i]) {
                    if (component.clickActionData != null && component.clickActionName != null) {
                        throw new IllegalArgumentException(
                            "The tooltip text cannot have click data.");
                    } else if (component.hoverActionData != null
                        && component.hoverActionName != null) {
                        throw new IllegalArgumentException(
                            "The tooltip text cannot have a tooltip.");
                    }
                    if (component.hasText()) {
                        result.messageParts.add(component.clone());
                        result.index = result.messageParts.size();
                    }
                }
                if (i != lines.length - 1) {
                    result.messageParts.add(new MessagePart(rawText("\n")));
                    result.index = result.messageParts.size();
                }
            } catch (CloneNotSupportedException e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to clone object", e);
                return this;
            }
        }
        return formattedTooltip(
            result.messageParts.isEmpty() ? null : result); // Throws NPE if size is 0, intended
    }

    /**
     * Set the behavior of the current editing component to display the specified lines of formatted
     * text when the client hovers over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the
     * message component on which they are applied.</p>
     *
     * @param lines The lines of text which will be displayed to the client upon hovering. The
     *              iteration order of this object will be the order in which the lines of the
     *              tooltip are created.
     * @return This builder instance.
     */
    public FancyMessage formattedTooltip(final Iterable<FancyMessage> lines) {
        return formattedTooltip(
            com.boydti.fawe.bukkit.chat.ArrayWrapper.toArray(lines, FancyMessage.class));
    }

    /**
     * If the text is a translatable key, and it has replaceable values, this function can be used
     * to set the replacements that will be used in the message.
     *
     * @param replacements The replacements, in order, that will be used in the language-specific
     *                     message.
     * @return This builder instance.
     */
    public FancyMessage translationReplacements(final String... replacements) {
        for (String str : replacements) {
            latest().translationReplacements.add(new JsonString(str));
        }
        dirty = true;

        return this;
    }
    /*

    /**
     * If the text is a translatable key, and it has replaceable values, this function can be used to set the replacements that will be used in the message.
     * @param replacements The replacements, in order, that will be used in the language-specific message.
     * @return This builder instance.
     */   /* ------------
    public FancyMessage translationReplacements(final Iterable<? extends CharSequence> replacements){
        for(CharSequence str : replacements){
            latest().translationReplacements.add(new JsonString(str));
        }

        return this;
    }

    */

    /**
     * If the text is a translatable key, and it has replaceable values, this function can be used
     * to set the replacements that will be used in the message.
     *
     * @param replacements The replacements, in order, that will be used in the language-specific
     *                     message.
     * @return This builder instance.
     */
    public FancyMessage translationReplacements(final FancyMessage... replacements) {
        Collections.addAll(latest().translationReplacements, replacements);

        dirty = true;

        return this;
    }

    /**
     * If the text is a translatable key, and it has replaceable values, this function can be used
     * to set the replacements that will be used in the message.
     *
     * @param replacements The replacements, in order, that will be used in the language-specific
     *                     message.
     * @return This builder instance.
     */
    public FancyMessage translationReplacements(final Iterable<FancyMessage> replacements) {
        return translationReplacements(
            com.boydti.fawe.bukkit.chat.ArrayWrapper.toArray(replacements, FancyMessage.class));
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new
     * message component. After a successful call to this method, all setter methods will refer to a
     * new message component, created as a result of the call to this method.
     *
     * @param text The text which will populate the new message component.
     * @return This builder instance.
     */
    public FancyMessage then(final String text) {
        return then(rawText(text));
    }

    private FancyMessage append(final String text) {
        if (!latest().hasText()) {
            throw new IllegalStateException("previous message part has no text");
        }
        MessagePart latest = latest();
        messageParts.add(new MessagePart(rawText(text)));
        latest().color = latest.color;
        latest().styles.addAll(latest.styles);
        dirty = true;
        return this;
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new
     * message component. After a successful call to this method, all setter methods will refer to a
     * new message component, created as a result of the call to this method.
     *
     * @param text The text which will populate the new message component.
     * @return This builder instance.
     */
    public FancyMessage then(final TextualComponent text) {
        if (!latest().hasText()) {
            throw new IllegalStateException("previous message part has no text");
        }
        messageParts.add(new MessagePart(text));
        index = messageParts.size();
        dirty = true;
        return this;
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new
     * message component. After a successful call to this method, all setter methods will refer to a
     * new message component, created as a result of the call to this method.
     *
     * @return This builder instance.
     */
    public FancyMessage then() {
        if (!latest().hasText()) {
            throw new IllegalStateException("previous message part has no text");
        }
        messageParts.add(new MessagePart());
        index = messageParts.size();
        dirty = true;
        return this;
    }

    @Override
    public void writeJson(JsonWriter writer) throws IOException {
        if (messageParts.size() == 1) {
            latest().writeJson(writer);
        } else {
            writer.beginObject().name("text").value("").name("extra").beginArray();
            for (final MessagePart part : this) {
                part.writeJson(writer);
            }
            writer.endArray().endObject();
        }
    }

    /**
     * Serialize this fancy message, converting it into syntactically-valid JSON using a {@link
     * JsonWriter}. This JSON should be compatible with vanilla formatter commands such as {@code
     * /tellraw}.
     *
     * @return The JSON string representing this object.
     */
    public String toJSONString() {
        if (!dirty && jsonString != null) {
            return jsonString;
        }
        StringWriter string = new StringWriter();
        JsonWriter json = new JsonWriter(string);
        try {
            writeJson(json);
            json.close();
        } catch (IOException e) {
            throw new RuntimeException("invalid message");
        }
        jsonString = string.toString();
        dirty = false;
        return jsonString;
    }

    /**
     * Sends this message to a player. The player will receive the fully-fledged formatted display
     * of this message.
     *
     * @param player The player who will receive the message.
     */
    public void send(Player player) {
        send(player, toJSONString());
    }

    private void send(CommandSender sender, String jsonString) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(toOldMessageFormat());
            return;
        }
        Player player = (Player) sender;
        try {
            Object handle = Reflection.getHandle(player);
            Object connection = Reflection.getField(handle.getClass(), "playerConnection")
                .get(handle);
            Reflection
                .getMethod(connection.getClass(), "sendPacket", Reflection.getNMSClass("Packet"))
                .invoke(connection, createChatPacket(jsonString));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().log(Level.WARNING, "Argument could not be passed.", e);
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().log(Level.WARNING, "Could not access method.", e);
        } catch (InstantiationException e) {
            Bukkit.getLogger().log(Level.WARNING, "Underlying class is abstract.", e);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger()
                .log(Level.WARNING, "A error has occurred during invoking of method.", e);
        } catch (NoSuchMethodException e) {
            Bukkit.getLogger().log(Level.WARNING, "Could not find method.", e);
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.WARNING, "Could not find class.", e);
        }
    }

    // The ChatSerializer's instance of Gson
    private static Object nmsChatSerializerGsonInstance;
    private static Method fromJsonMethod;

    private Object createChatPacket(String json)
        throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        if (nmsChatSerializerGsonInstance == null) {
            // Find the field and its value, completely bypassing obfuscation
            Class<?> chatSerializerClazz;

            // Get the three parts of the version string (major version is currently unused)
            // vX_Y_RZ
            //   X = major
            //   Y = minor
            //   Z = revision
            final String version = Reflection.getVersion();
            String[] split = version.substring(1, version.length() - 1)
                .split("_"); // Remove trailing dot
            //int majorVersion = Integer.parseInt(split[0]);
            int minorVersion = Integer.parseInt(split[1]);
            int revisionVersion = Integer.parseInt(split[2].substring(1)); // Substring to ignore R

            if (minorVersion < 8 || (minorVersion == 8 && revisionVersion == 1)) {
                chatSerializerClazz = Reflection.getNMSClass("ChatSerializer");
            } else {
                chatSerializerClazz = Reflection.getNMSClass("IChatBaseComponent$ChatSerializer");
            }

            if (chatSerializerClazz == null) {
                throw new ClassNotFoundException("Can't find the ChatSerializer class");
            }

            for (Field declaredField : chatSerializerClazz.getDeclaredFields()) {
                if (Modifier.isFinal(declaredField.getModifiers()) && Modifier
                    .isStatic(declaredField.getModifiers()) && declaredField.getType().getName()
                    .endsWith("Gson")) {
                    // We've found our field
                    declaredField.setAccessible(true);
                    nmsChatSerializerGsonInstance = declaredField.get(null);
                    fromJsonMethod = nmsChatSerializerGsonInstance.getClass()
                        .getMethod("fromJson", String.class, Class.class);
                    break;
                }
            }
        }

        // Since the method is so simple, and all the obfuscated methods have the same name, it's easier to reimplement 'IChatBaseComponent a(String)' than to reflectively call it
        // Of course, the implementation may change, but fuzzy matches might break with signature changes
        Object serializedChatComponent = fromJsonMethod.invoke(nmsChatSerializerGsonInstance, json,
            Reflection.getNMSClass("IChatBaseComponent"));

        return nmsPacketPlayOutChatConstructor.newInstance(serializedChatComponent);
    }

    /**
     * Sends this message to a command sender. If the sender is a player, they will receive the
     * fully-fledged formatted display of this message. Otherwise, they will receive a version of
     * this message with less formatting.
     *
     * @param sender The command sender who will receive the message.
     * @see #toOldMessageFormat()
     */
    public void send(CommandSender sender) {
        send(sender, toJSONString());
    }

    /**
     * Sends this message to multiple command senders.
     *
     * @param senders The command senders who will receive the message.
     * @see #send(CommandSender)
     */
    public void send(final Iterable<? extends CommandSender> senders) {
        String string = toJSONString();
        for (final CommandSender sender : senders) {
            send(sender, string);
        }
    }

    /**
     * Convert this message to a human-readable string with limited formatting. This method is used
     * to send this message to clients without JSON formatting support.
     * <p>
     * Serialization of this message by using this message will include (in this order for each
     * message part):
     * <ol>
     * <li>The color of each message part.</li>
     * <li>The applicable stylizations for each message part.</li>
     * <li>The core text of the message part.</li>
     * </ol>
     * The primary omissions are tooltips and clickable actions. Consequently, this method should be used only as a last resort.
     * </p>
     * <p>
     * Color and formatting can be removed from the returned string by using {@link ChatColor#stripColor(String)}.</p>
     *
     * @return A human-readable string representing limited formatting in addition to the core text
     * of this message.
     */
    public String toOldMessageFormat() {
        StringBuilder result = new StringBuilder();
        for (MessagePart part : this) {
            result.append(part.color == null ? "" : part.color);
            for (ChatColor formatSpecifier : part.styles) {
                result.append(formatSpecifier);
            }
            result.append(part.text);
        }
        return result.toString();
    }

    private void onCurrent(Consumer<MessagePart> call) {
        for (int i = index - 1; i < messageParts.size(); i++) {
            call.accept(messageParts.get(i));
        }
    }

    private MessagePart latest() {
        return messageParts.get(messageParts.size() - 1);
    }

    private void onClick(final String name, final String data) {
        onCurrent(m -> {
            m.clickActionName = name;
            m.clickActionData = data;
        });
        dirty = true;
    }

    private void onHover(final String name, final JsonRepresentedObject data) {
        onCurrent(m -> {
            m.hoverActionName = name;
            m.hoverActionData = data;
        });
        dirty = true;
    }

    // Doc copied from interface
    public Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("messageParts", messageParts);
//		map.put("JSON", toJSONString());
        return map;
    }

    /**
     * Deserializes a JSON-represented message from a mapping of key-value pairs. This is called by
     * the Bukkit serialization API. It is not intended for direct public API consumption.
     *
     * @param serialized The key-value mapping which represents a fancy message.
     */
    @SuppressWarnings("unchecked")
    public static FancyMessage deserialize(Map<String, Object> serialized) {
        FancyMessage msg = new FancyMessage();
        msg.messageParts = (List<MessagePart>) serialized.get("messageParts");
        msg.jsonString = serialized.containsKey("JSON") ? serialized.get("JSON").toString() : null;
        msg.dirty = !serialized.containsKey("JSON");
        return msg;
    }

    /**
     * <b>Internally called method. Not for API consumption.</b>
     */
    @NotNull
    public Iterator<MessagePart> iterator() {
        return messageParts.iterator();
    }

    private static JsonParser _stringParser = new JsonParser();

    /**
     * Deserializes a fancy message from its JSON representation. This JSON representation is of the
     * format of that returned by {@link #toJSONString()}, and is compatible with vanilla inputs.
     *
     * @param json The JSON string which represents a fancy message.
     * @return A {@code FancyMessage} representing the parameterized JSON message.
     */
    public static FancyMessage deserialize(String json) {
        JsonObject serialized = _stringParser.parse(json).getAsJsonObject();
        JsonArray extra = serialized.getAsJsonArray("extra"); // Get the extra component
        FancyMessage returnVal = new FancyMessage();
        returnVal.messageParts.clear();
        for (JsonElement mPrt : extra) {
            MessagePart component = new MessagePart();
            JsonObject messagePart = mPrt.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : messagePart.entrySet()) {
                // Deserialize text
                if (TextualComponent.isTextKey(entry.getKey())) {
                    // The map mimics the YAML serialization, which has a "key" field and one or more "value" fields
                    Map<String, Object> serializedMapForm = new HashMap<>(); // Must be object due to Bukkit serializer API compliance
                    serializedMapForm.put("key", entry.getKey());
                    if (entry.getValue().isJsonPrimitive()) {
                        // Assume string
                        serializedMapForm.put("value", entry.getValue().getAsString());
                    } else {
                        // Composite object, but we assume each element is a string
                        for (Map.Entry<String, JsonElement> compositeNestedElement : entry
                            .getValue().getAsJsonObject().entrySet()) {
                            serializedMapForm.put("value." + compositeNestedElement.getKey(),
                                compositeNestedElement.getValue().getAsString());
                        }
                    }
                    component.text = TextualComponent.deserialize(serializedMapForm);
                } else if (MessagePart.stylesToNames.inverse().containsKey(entry.getKey())) {
                    if (entry.getValue().getAsBoolean()) {
                        component.styles
                            .add(MessagePart.stylesToNames.inverse().get(entry.getKey()));
                    }
                } else if (entry.getKey().equals("color")) {
                    component.color = ChatColor
                        .valueOf(entry.getValue().getAsString().toUpperCase());
                } else if (entry.getKey().equals("clickEvent")) {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    component.clickActionName = object.get("action").getAsString();
                    component.clickActionData = object.get("value").getAsString();
                } else if (entry.getKey().equals("hoverEvent")) {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    component.hoverActionName = object.get("action").getAsString();
                    if (object.get("value").isJsonPrimitive()) {
                        // Assume string
                        component.hoverActionData = new JsonString(
                            object.get("value").getAsString());
                    } else {
                        // Assume composite type
                        // The only composite type we currently store is another FancyMessage
                        // Therefore, recursion time!
                        component.hoverActionData = deserialize(object.get("value")
                            .toString() /* This should properly serialize the JSON object as a JSON string */);
                    }
                } else if (entry.getKey().equals("insertion")) {
                    component.insertionData = entry.getValue().getAsString();
                } else if (entry.getKey().equals("with")) {
                    for (JsonElement object : entry.getValue().getAsJsonArray()) {
                        if (object.isJsonPrimitive()) {
                            component.translationReplacements
                                .add(new JsonString(object.getAsString()));
                        } else {
                            // Only composite type stored in this array is - again - FancyMessages
                            // Recurse within this function to parse this as a translation replacement
                            component.translationReplacements.add(deserialize(object.toString()));
                        }
                    }
                }
            }
            returnVal.messageParts.add(component);
            returnVal.index = returnVal.messageParts.size();
        }
        return returnVal;
    }

}
