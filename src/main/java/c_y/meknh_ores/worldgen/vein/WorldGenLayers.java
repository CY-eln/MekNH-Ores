package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.MekNHOres;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of world generation layers.  The four built-in layers mirror GregTech's
 * stone / deepslate / netherrack / endstone layers.
 */
public final class WorldGenLayers {

    public static final TagKey<Block> ENDSTONE_ORE_REPLACEABLES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, "endstone_ore_replaceables"));

    private static final Map<ResourceLocation, WorldGenLayer> LAYERS = new LinkedHashMap<>();

    public static final WorldGenLayer STONE = register("stone",
            new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
            Set.of(Level.OVERWORLD.location()));

    public static final WorldGenLayer DEEPSLATE = register("deepslate",
            new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
            Set.of(Level.OVERWORLD.location()));

    public static final WorldGenLayer NETHERRACK = register("netherrack",
            new TagMatchTest(BlockTags.NETHER_CARVER_REPLACEABLES),
            Set.of(Level.NETHER.location()));

    public static final WorldGenLayer ENDSTONE = register("endstone",
            new TagMatchTest(ENDSTONE_ORE_REPLACEABLES),
            Set.of(Level.END.location()));

    private WorldGenLayers() {
    }

    public static synchronized WorldGenLayer register(String id, RuleTest target,
                                                      Set<ResourceLocation> dimensions) {
        ResourceLocation key = parseId(id);
        WorldGenLayer layer = new WorldGenLayer(key, target, dimensions);
        LAYERS.put(key, layer);
        return layer;
    }

    public static synchronized WorldGenLayer get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return LAYERS.get(parseId(id));
    }

    public static synchronized Collection<WorldGenLayer> all() {
        return List.copyOf(LAYERS.values());
    }

    public static synchronized WorldGenLayer getOrThrow(String id) {
        WorldGenLayer layer = get(id);
        if (layer == null) {
            throw new IllegalArgumentException("Unknown world generation layer: " + id);
        }
        return layer;
    }

    public static ResourceLocation parseId(String id) {
        ResourceLocation parsed = id != null && id.indexOf(':') >= 0
                ? ResourceLocation.tryParse(id)
                : ResourceLocation.tryParse(MekNHOres.MODID + ":" + id);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid resource location: " + id);
        }
        return parsed;
    }

    /** A RuleTest that matches any of the supplied rules. */
    public static final class CompositeRuleTest extends RuleTest {

        private final List<RuleTest> rules;

        public CompositeRuleTest(Collection<RuleTest> rules) {
            this.rules = List.copyOf(rules);
        }

        @Override
        public boolean test(BlockState state, RandomSource random) {
            for (RuleTest rule : rules) {
                if (rule.test(state, random)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType<?> getType() {
            // Composite rules are runtime-only and are never serialized.
            return net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType.ALWAYS_TRUE_TEST;
        }
    }

    public static RuleTest ruleFor(String target) {
        if (target.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
            if (tag == null) {
                throw new IllegalArgumentException("Invalid block tag: " + target);
            }
            return new TagMatchTest(TagKey.create(Registries.BLOCK, tag));
        }
        ResourceLocation blockId = parseId(target);
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockId)) {
            throw new IllegalArgumentException("Unknown block: " + target);
        }
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockId);
        return new BlockMatchTest(block);
    }

    public static RuleTest combinedRuleFor(List<String> targets) {
        List<RuleTest> rules = new ArrayList<>(targets.size());
        for (String target : targets) {
            rules.add(ruleFor(target));
        }
        if (rules.isEmpty()) {
            return new BlockMatchTest(net.minecraft.world.level.block.Blocks.AIR);
        }
        return rules.size() == 1 ? rules.get(0) : new CompositeRuleTest(rules);
    }
}
