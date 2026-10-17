import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Keeps Zombie Extreme's mobs while removing its radioactive biome injection. */
public final class PatchZombieExtreme {
    private static final String BIOME_CLASS = "zombie_extreme/init/ZombieExtremeModBiomes.class";
    private static final String RADIATION_CLASS = "zombie_extreme/procedures/RadiationBiomeEffectProcedure.class";

    private PatchZombieExtreme() {}

    private static byte[] neutralize(byte[] bytes, String methodName) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals(methodName)) continue;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = 0;
            count++;
        }
        if (count != 1) throw new IllegalStateException("Expected one method " + methodName + ", found " + count);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: PatchZombieExtreme original.jar patched.jar");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Set<String> patched = new HashSet<>();
        int skippedStructureSets = 0;
        try (ZipFile zip = new ZipFile(input.toFile()); OutputStream stream = Files.newOutputStream(output);
             ZipOutputStream target = new ZipOutputStream(stream)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("data/zombie_extreme/worldgen/structure_set/")
                    && entry.getName().endsWith(".json")) {
                    skippedStructureSets++;
                    continue;
                }
                target.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream source = zip.getInputStream(entry)) { bytes = source.readAllBytes(); }
                    if (entry.getName().equals(BIOME_CLASS)) {
                        bytes = neutralize(bytes, "onServerAboutToStart");
                        patched.add(BIOME_CLASS);
                    } else if (entry.getName().equals(RADIATION_CLASS)) {
                        bytes = neutralize(bytes, "onPlayerTick");
                        patched.add(RADIATION_CLASS);
                    }
                    target.write(bytes);
                }
                target.closeEntry();
            }
        }
        if (!patched.contains(BIOME_CLASS) || !patched.contains(RADIATION_CLASS) || skippedStructureSets != 10) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Unexpected Zombie Extreme version: classes=" + patched
                + ", structure sets=" + skippedStructureSets);
        }
        System.out.println("Patched biome injection and radiation tick; removed "
            + skippedStructureSets + " native structure sets. Mobs and registry retained.");
    }
}
