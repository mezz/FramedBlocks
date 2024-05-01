package xfacthd.framedblocks.common.data.camo;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.*;
import xfacthd.framedblocks.common.FBContent;

import java.util.*;
import java.util.function.Predicate;

public final class CamoContainerFactories
{
    private static final Map<Item, CamoContainerFactory<?>> APPLICATION_ITEMS = new IdentityHashMap<>();
    private static final List<FactoryPredicatePair> APPLICATION_PREDICATES = new ArrayList<>();
    private static final Map<Item, Set<CamoContainerFactory<?>>> REMOVAL_ITEMS = new IdentityHashMap<>();
    private static final List<FactoryPredicatePair> REMOVAL_PREDICATES = new ArrayList<>();

    public static void registerCamoFactories()
    {
        FBContent.CAMO_CONTAINER_FACTORY_REGISTRY
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .forEach(factory -> factory.registerTriggerItems(new TriggerRegistrarImpl(factory)));

        // Register builtin handling last to make sure the predicates actually act as broad fallbacks after addon ones

        TriggerRegistrar fluidRegistrar = new TriggerRegistrarImpl(FBContent.FACTORY_FLUID.value());
        fluidRegistrar.registerApplicationPredicate(stack -> stack.getCapability(Capabilities.FluidHandler.ITEM) != null);
        fluidRegistrar.registerRemovalPredicate(stack -> stack.getCapability(Capabilities.FluidHandler.ITEM) != null);

        TriggerRegistrar blockRegistrar = new TriggerRegistrarImpl(FBContent.FACTORY_BLOCK.value());
        blockRegistrar.registerApplicationPredicate(stack -> stack.getItem() instanceof BlockItem);
        blockRegistrar.registerRemovalItem(FBContent.ITEM_FRAMED_HAMMER.value());
    }

    @Nullable
    public static CamoContainerFactory<?> findCamoFactory(ItemStack stack)
    {
        CamoContainerFactory<?> factory = APPLICATION_ITEMS.get(stack.getItem());
        if (factory == null)
        {
            for (FactoryPredicatePair pair : APPLICATION_PREDICATES)
            {
                if (pair.predicate.test(stack))
                {
                    factory = pair.factory;
                    break;
                }
            }
        }
        return factory;
    }

    public static boolean isValidRemovalTool(CamoContainer<?, ?> container, ItemStack stack)
    {
        Set<CamoContainerFactory<?>> factories = REMOVAL_ITEMS.get(stack.getItem());
        if (factories.contains(container.getFactory()))
        {
            return true;
        }

        for (FactoryPredicatePair pair : REMOVAL_PREDICATES)
        {
            if (pair.predicate.test(stack) && pair.factory == container.getFactory())
            {
                return true;
            }
        }
        return false;
    }



    private record TriggerRegistrarImpl(CamoContainerFactory<?> factory) implements TriggerRegistrar
    {
        @Override
        public void registerApplicationItem(Item item)
        {
            if (APPLICATION_ITEMS.containsKey(item))
            {
                throw new IllegalArgumentException(String.format("Item %s is already registered!", item));
            }
            APPLICATION_ITEMS.put(item, factory);
        }

        @Override
        public void registerApplicationPredicate(Predicate<ItemStack> predicate)
        {
            APPLICATION_PREDICATES.add(new FactoryPredicatePair(predicate, factory));
        }

        @Override
        public void registerRemovalItem(Item item)
        {
            Set<CamoContainerFactory<?>> factories = REMOVAL_ITEMS.computeIfAbsent(item, $ -> new ReferenceOpenHashSet<>());
            if (factories.contains(factory))
            {
                String factoryName = Objects.requireNonNull(FBContent.CAMO_CONTAINER_FACTORY_REGISTRY.getKey(factory)).toString();
                throw new IllegalArgumentException(String.format("Factory %s is already registered to item %s!", factoryName, item));
            }
            factories.add(factory);
        }

        @Override
        public void registerRemovalPredicate(Predicate<ItemStack> predicate)
        {
            REMOVAL_PREDICATES.add(new FactoryPredicatePair(predicate, factory));
        }
    }

    private record FactoryPredicatePair(Predicate<ItemStack> predicate, CamoContainerFactory<?> factory) { }



    private CamoContainerFactories() { }
}
