package io.github.shaksternano.wmitaf.client.util.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ModNameUtil {

    /**
     * The Item ID of Extra Alchemy's breakable potion.
     */
    private static final String EXTRA_ALCHEMY_BREAKABLE_POTION_ID = "extraalchemy:breakable_potion";
    /**
     * The Item ID of Patchouli's guide book.
     */
    private static final String PATCHOULI_BOOK_ID = "patchouli:guide_book";
    /**
     * The ID of the NBT tag storing the Patchouli book ID.
     */
    private static final String PATCHOULI_BOOK_TAG = "patchouli:book";

    /**
     * Gets the name of the mod that adds the first enchantment on
     * an enchanted book ItemStack or the first effect on a potion.
     *
     * @param stack The ItemStack to get the mod name of.
     * @return An {@link Optional} describing the name of the mod that
     * adds the first enchantment on an enchanted book ItemStack or
     * the first effect on a potion.
     */
    public static Optional<String> getActualModName(ItemStack stack) {
        if (modNameNeedsToBeChanged(stack)) {
            return getActualModId(stack).map(modId -> getModNameFromId(modId).orElseGet(() -> StringUtils.capitalize(modId)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the ID of the mod that adds the first enchantment on
     * an enchanted book ItemStack or the first effect on a potion.
     *
     * @param stack The ItemStack to get the mod name of.
     * @return An {@link Optional} that describes the ID of the mod
     * that adds the first enchantment on an enchanted book ItemStack
     * or the first effect on a potion.
     */
    public static Optional<String> getActualModId(ItemStack stack) {
        if (modNameNeedsToBeChanged(stack)) {
            return getIdentifierFromStackData(stack).map(Identifier::getNamespace);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Determines if an {@link ItemStack} should have its mod name changed.
     *
     * @param stack The ItemStack to check.
     * @return {@code true} if the ItemStack should have its mod name changed, {@code false} otherwise.
     */
    public static boolean modNameNeedsToBeChanged(ItemStack stack) {
        return stack.isOf(Items.ENCHANTED_BOOK) ||
                hasStatusEffects(stack) ||
                hasId(stack.getItem(), PATCHOULI_BOOK_ID);
    }

    /**
     * Gets the {@link Identifier} of the first enchantment on an enchanted book,
     * or the Identifier of the first effect on a potion.
     *
     * @param stack The ItemStack to get the Identifier of.
     * @return An {@link Optional} that describes the Identifier of the first enchantment.
     */
    private static Optional<Identifier> getIdentifierFromStackData(ItemStack stack) {
        Optional<Identifier> identifierOptional = Optional.empty();
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            identifierOptional = getFirstEnchantmentId(stack);
        } else if (hasStatusEffects(stack)) {
            identifierOptional = getFirstEffectId(stack);
        } else if (hasId(stack.getItem(), PATCHOULI_BOOK_ID)) {
            identifierOptional = getPatchouliBookId(stack);
        }
        return identifierOptional;
    }

    /**
     * Gets the name of a mod from the mod's ID.
     *
     * @param namespace The mod's ID.
     * @return An {@link Optional} that describes the name of the mod.
     */
    public static Optional<String> getModNameFromId(String namespace) {
        return FabricLoader.getInstance()
                .getModContainer(namespace)
                .map(container -> container.getMetadata().getName());
    }

    /**
     * Gets the {@link Identifier} of the first enchantment on an {@link ItemStack}.
     *
     * @param stack The ItemStack to get the Identifier of.
     * @return An {@link Optional} that describes the Identifier of the first enchantment.
     */
    private static Optional<Identifier> getFirstEnchantmentId(ItemStack stack) {
        Set<Enchantment> enchantments = EnchantmentHelper.get(stack).keySet();
        for (Enchantment enchantment : enchantments) {
            Identifier enchantmentId = EnchantmentHelper.getEnchantmentId(enchantment);
            if (enchantmentId != null) {
                return Optional.of(enchantmentId);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the {@link Identifier} of the first effect on a potion.
     *
     * @param stack The ItemStack to get the Identifier of.
     * @return An {@link Optional} that describes the Identifier of the first effect.
     */
    private static Optional<Identifier> getFirstEffectId(ItemStack stack) {
        List<StatusEffectInstance> effectInstances = PotionUtil.getPotionEffects(stack);
        for (StatusEffectInstance effectInstance : effectInstances) {
            StatusEffect effect = effectInstance.getEffectType();
            Identifier effectId = Registry.STATUS_EFFECT.getId(effect);
            if (effectId != null) {
                // Don't make potion types from mods with vanilla effects seem like they're from vanilla, for example a breakable potion from Extra Alchemy with the vanilla night vision effect.
                if (effectId.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                    return Optional.of(Registry.ITEM.getId(stack.getItem()));
                } else {
                    return Optional.of(effectId);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the book {@link Identifier} in stored in the NBT of a Patchouli book.
     *
     * @param stack The {@link ItemStack} to get the Identifier of.
     * @return An {@link Optional} that describes the Identifier of the book.
     */
    @SuppressWarnings("ConstantConditions")
    private static Optional<Identifier> getPatchouliBookId(ItemStack stack) {
        if (stack.hasNbt()) {
            String bookId = stack.getNbt().getString(PATCHOULI_BOOK_TAG);
            if (!bookId.equals("")) {
                return Optional.of(new Identifier(bookId));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns true if the {@link ItemStack} is a type of item that carries
     * status effects, otherwise returns false.
     *
     * @param stack The ItemStack to check.
     * @return True if the ItemStack is a type of item that carries status
     * effects, otherwise false.
     */
    private static boolean hasStatusEffects(ItemStack stack) {
        return stack.isOf(Items.POTION) ||
                stack.isOf(Items.SPLASH_POTION) ||
                stack.isOf(Items.LINGERING_POTION) ||
                stack.isOf(Items.TIPPED_ARROW) ||
                hasId(stack.getItem(), EXTRA_ALCHEMY_BREAKABLE_POTION_ID);
    }

    /**
     * Checks if the ID of the item matches the string passed as an argument.
     *
     * @param item The item to check.
     * @param id   The ID to check against.
     * @return True if the ID of the item matches the string passed as an argument.
     */
    private static boolean hasId(Item item, String id) {
        return Registry.ITEM.getId(item).toString().equals(id);
    }
}
