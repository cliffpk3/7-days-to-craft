package dev.sevendaystocraft.skillbridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.puffish.skillsmod.api.SkillsAPI;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Refunds allocated Survivor skill points once for every semantic revision of
 * the active tree files. Experience and skill level are owned by Puffish's
 * Experience object and are deliberately left untouched.
 */
final class SkillTreeRevisionReset {
    private static final ResourceLocation SURVIVOR =
        new ResourceLocation("seven_days_to_craft", "survivor");
    private static final String STATE_FILE = "7dtc-skill-tree-revision.json";
    private static final int FORMAT = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> TREE_FILES = List.of(
        "category.json",
        "connections.json",
        "definitions.json",
        "experience.json",
        "skills.json"
    );

    private SkillTreeRevisionReset() {}

    static synchronized void apply(ServerPlayer player) {
        Optional<String> fingerprint = fingerprint();
        if (fingerprint.isEmpty()) return;

        Path statePath = player.server.getWorldPath(LevelResource.ROOT)
            .resolve("serverconfig")
            .resolve(STATE_FILE);

        try {
            State state = readState(statePath);

            // Installing this feature must not reset an unchanged existing world.
            // The first successful read establishes revision zero as its baseline.
            if (state == null) {
                state = new State(fingerprint.get(), 0, new JsonObject());
                state.players.addProperty(player.getUUID().toString(), 0);
                writeState(statePath, state);
                return;
            }

            boolean changed = false;
            if (!state.hash.equals(fingerprint.get())) {
                state.hash = fingerprint.get();
                state.revision++;
                changed = true;
                SkillBridge.LOGGER.info(
                    "Detected Survivor skill-tree revision {} for world {}",
                    state.revision,
                    player.server.getWorldPath(LevelResource.ROOT).getFileName()
                );
            }

            String playerId = player.getUUID().toString();
            int playerRevision = state.players.has(playerId)
                ? state.players.get(playerId).getAsInt()
                : (state.revision == 0 ? 0 : -1);

            if (playerRevision < state.revision) {
                var category = SkillsAPI.getCategory(SURVIVOR);
                if (category.isEmpty()) {
                    SkillBridge.LOGGER.warn("Survivor skill category is unavailable; postponing point refund for {}", player.getGameProfile().getName());
                    if (changed) writeState(statePath, state);
                    return;
                }
                int spentPoints = category.get().getSpentPoints(player);
                if (spentPoints > 0) {
                    category.get().resetSkills(player);
                    player.displayClientMessage(
                        Component.literal("The skill tree changed. Your spent skill points were refunded.")
                            .withStyle(ChatFormatting.YELLOW),
                        false
                    );
                    SkillBridge.LOGGER.info(
                        "Refunded {} spent Survivor skill points for {} at tree revision {}",
                        spentPoints,
                        player.getGameProfile().getName(),
                        state.revision
                    );
                }
                state.players.addProperty(playerId, state.revision);
                changed = true;
            } else if (!state.players.has(playerId)) {
                state.players.addProperty(playerId, state.revision);
                changed = true;
            }

            if (changed) writeState(statePath, state);
        } catch (Exception exception) {
            // A damaged/missing state must never cause an accidental mass reset.
            SkillBridge.LOGGER.error("Could not process automatic skill-tree point refund", exception);
        }
    }

    private static Optional<String> fingerprint() {
        Path treeDir = FMLPaths.GAMEDIR.get()
            .resolve("kubejs")
            .resolve("data")
            .resolve("seven_days_to_craft")
            .resolve("puffish_skills")
            .resolve("categories")
            .resolve("survivor");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String fileName : TREE_FILES) {
                Path file = treeDir.resolve(fileName);
                if (!Files.isRegularFile(file)) {
                    SkillBridge.LOGGER.error("Cannot fingerprint skill tree: missing {}", file);
                    return Optional.empty();
                }
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement normalized = JsonParser.parseReader(reader);
                    digest.update(fileName.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    digest.update(normalized.toString().getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                }
            }
            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException | RuntimeException exception) {
            SkillBridge.LOGGER.error("Could not fingerprint active Survivor skill tree", exception);
            return Optional.empty();
        }
    }

    private static State readState(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            int format = root.has("format") ? root.get("format").getAsInt() : 0;
            if (format != FORMAT) throw new IOException("Unsupported skill-tree revision format: " + format);
            String hash = root.get("hash").getAsString();
            int revision = root.get("revision").getAsInt();
            JsonObject players = root.has("players") && root.get("players").isJsonObject()
                ? root.getAsJsonObject("players")
                : new JsonObject();
            return new State(hash, revision, players);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid skill-tree revision state at " + path, exception);
        }
    }

    private static void writeState(Path path, State state) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("hash", state.hash);
        root.addProperty("revision", state.revision);
        root.add("players", state.players);

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class State {
        private String hash;
        private int revision;
        private final JsonObject players;

        private State(String hash, int revision, JsonObject players) {
            this.hash = hash;
            this.revision = revision;
            this.players = players;
        }
    }
}
