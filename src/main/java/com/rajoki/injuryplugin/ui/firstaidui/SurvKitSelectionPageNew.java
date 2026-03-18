package com.rajoki.injuryplugin.ui.firstaidui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.config.MortalWoundsConfig;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.utils.HealthColorUtil;
import com.rajoki.injuryplugin.utils.HealthSyncUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SurvKitSelectionPageNew extends InteractiveCustomUIPage<SurvKitSelectionPageNew.SurvKitEventData> {

    private static final ScheduledExecutorService scheduler;

    static {
        scheduler = Executors.newScheduledThreadPool(1);
    }
    private ScheduledFuture<?> refreshTask;
    private final PlayerRef playerRef;

    public SurvKitSelectionPageNew(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SurvKitEventData.CODEC);
        this.playerRef = playerRef;
    }

    private static final BodyPart[] UI_PARTS = {
            BodyPart.HEAD,
            BodyPart.TORSO,
            BodyPart.LEFTARM,
            BodyPart.RIGHTARM,
            BodyPart.LEFTLEG,
            BodyPart.RIGHTLEG
    };

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        SurvKitUIRegistry.register(playerRef, this);

        uiCommandBuilder.append("SurvKitSelection.ui");

        BodyPartComponent bodyPartData = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartData != null) {
            // Update all visual elements
            updateBodyPartModel(uiCommandBuilder, "#HeadModel", bodyPartData, BodyPart.HEAD);
            updateBodyPartModel(uiCommandBuilder, "#TorsoModel", bodyPartData, BodyPart.TORSO);
            updateBodyPartModel(uiCommandBuilder, "#RightArmModel", bodyPartData, BodyPart.RIGHTARM);
            updateBodyPartModel(uiCommandBuilder, "#LeftArmModel", bodyPartData, BodyPart.LEFTARM);
            updateBodyPartModel(uiCommandBuilder, "#RightLegModel", bodyPartData, BodyPart.RIGHTLEG);
            updateBodyPartModel(uiCommandBuilder, "#LeftLegModel", bodyPartData, BodyPart.LEFTLEG);

            updateLabel(uiCommandBuilder, "#HeadHealth", bodyPartData, BodyPart.HEAD);
            updateLabel(uiCommandBuilder, "#TorsoHealth", bodyPartData, BodyPart.TORSO);
            updateLabel(uiCommandBuilder, "#RightArmHealth", bodyPartData, BodyPart.RIGHTARM);
            updateLabel(uiCommandBuilder, "#LeftArmHealth", bodyPartData, BodyPart.LEFTARM);
            updateLabel(uiCommandBuilder, "#RightLegHealth", bodyPartData, BodyPart.RIGHTLEG);
            updateLabel(uiCommandBuilder, "#LeftLegHealth", bodyPartData, BodyPart.LEFTLEG);

            // Update body part health bars
            updateProgressBar(uiCommandBuilder, "#TorsoBar", bodyPartData, BodyPart.TORSO);
            updateProgressBar(uiCommandBuilder, "#HeadBar", bodyPartData, BodyPart.HEAD);
            updateProgressBar(uiCommandBuilder, "#RightArmBar", bodyPartData, BodyPart.RIGHTARM);
            updateProgressBar(uiCommandBuilder, "#LeftArmBar", bodyPartData, BodyPart.LEFTARM);
            updateProgressBar(uiCommandBuilder, "#RightLegBar", bodyPartData, BodyPart.RIGHTLEG);
            updateProgressBar(uiCommandBuilder, "#LeftLegBar", bodyPartData, BodyPart.LEFTLEG);

            // Update status icons
            updateFractureIcons(uiCommandBuilder, bodyPartData);
            updateBleedIcons(uiCommandBuilder, bodyPartData);
            updateHeavyBleedIcons(uiCommandBuilder, bodyPartData);

            // Bind button events for repairing broken limbs
            bindSurvKitButtons(uiEventBuilder, bodyPartData);

            // Disable buttons for parts that aren't broken
            disableButtonsForHealthyParts(uiCommandBuilder, bodyPartData);
        }

        startAutoRefresh(ref, store);
    }

    private void bindSurvKitButtons(UIEventBuilder eventBuilder, BodyPartComponent bodyPartData) {
        // Add buttons for each broken body part
        for (BodyPart part : UI_PARTS) {
            boolean isBroken = bodyPartData.isBodyPartBroken(part); // ← Changed from hasBodyPartEffect

            String buttonId = "#" + getButtonId(part);

            if (isBroken) {
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        buttonId,
                        EventData.of("Part", part.name())
                );
            }
        }
    }

    private String getButtonId(BodyPart part) {
        return switch (part) {
            case HEAD -> "HeadSurgeryButton";
            case TORSO -> "TorsoSurgeryButton";
            case LEFTARM -> "LeftArmSurgeryButton";
            case RIGHTARM -> "RightArmSurgeryButton";
            case LEFTLEG -> "LeftLegSurgeryButton";
            case RIGHTLEG -> "RightLegSurgeryButton";
            default -> "";
        };
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SurvKitEventData data) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.part != null) {
            try {
                BodyPart part = BodyPart.valueOf(data.part);

                BodyPartComponent bodyPartComp = store.getComponent(ref,
                        MortalWoundsPlugin.getInstance().getBodyPartComponentType());

                // Check if part is broken
                if (bodyPartComp != null && bodyPartComp.isBodyPartBroken(part)) {

                    // Consume survival kit
                    if (!consumeSurvivalKit(player)) {
                        playerRef.sendMessage(Message.raw("You need a Survival Kit!"));
                        return;
                    }


                    // // Remove broken status
                    // bodyPartComp.setBodyPartBroken(part, false);
                    // // Remove DESTROYED effect if present
                    // bodyPartComp.removeBodyPartEffect(part, "DESTROYED");
                    // // Restore limb to 1 HP
                    // bodyPartComp.setBodyPartHealth(part, 1.0f);

                    //This Util should handle the same as above commented out
                    HealthSyncUtil.healBrokenLimb(ref, store, bodyPartComp, part);


//                    playerRef.sendMessage(Message.raw(
//                            String.format("§aRepaired %s — limb restored!", part.getDisplayName())
//                    ));
//
//                    MortalWoundsPlugin.getPluginLogger().atInfo().log(
//                            String.format("[SURVKIT] Repaired broken %s", part.getDisplayName())
//                    );
                }

                // Don't close the page, just refresh it
                refresh(ref, store);

                // Keep the page open after using item if true, close if false
                if (!MortalWoundsConfig.get().treatMultipleWounds) {
                    player.getPageManager().setPage(ref, store, Page.None);
                }

            } catch (IllegalArgumentException e) {
                MortalWoundsPlugin.getPluginLogger().atWarning().log(
                        "Invalid body part in survival kit event: " + data.part
                );
            }
        }
    }

    private void startAutoRefresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (refreshTask != null) {
            refreshTask.cancel(false);
        }

        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                Player player = (Player) store.getComponent(ref, Player.getComponentType());

                if (player != null && !player.wasRemoved()) {
                    BodyPartComponent bodyPartData = store.getComponent(ref,
                            MortalWoundsPlugin.getInstance().getBodyPartComponentType());

                    if (bodyPartData != null) {
                        UICommandBuilder commandBuilder = new UICommandBuilder();

                        updateBodyPartModel(commandBuilder, "#HeadModel", bodyPartData, BodyPart.HEAD);
                        updateBodyPartModel(commandBuilder, "#TorsoModel", bodyPartData, BodyPart.TORSO);
                        updateBodyPartModel(commandBuilder, "#RightArmModel", bodyPartData, BodyPart.RIGHTARM);
                        updateBodyPartModel(commandBuilder, "#LeftArmModel", bodyPartData, BodyPart.LEFTARM);
                        updateBodyPartModel(commandBuilder, "#RightLegModel", bodyPartData, BodyPart.RIGHTLEG);
                        updateBodyPartModel(commandBuilder, "#LeftLegModel", bodyPartData, BodyPart.LEFTLEG);

                        updateLabel(commandBuilder, "#HeadHealth", bodyPartData, BodyPart.HEAD);
                        updateLabel(commandBuilder, "#TorsoHealth", bodyPartData, BodyPart.TORSO);
                        updateLabel(commandBuilder, "#RightArmHealth", bodyPartData, BodyPart.RIGHTARM);
                        updateLabel(commandBuilder, "#LeftArmHealth", bodyPartData, BodyPart.LEFTARM);
                        updateLabel(commandBuilder, "#RightLegHealth", bodyPartData, BodyPart.RIGHTLEG);
                        updateLabel(commandBuilder, "#LeftLegHealth", bodyPartData, BodyPart.LEFTLEG);

                        updateProgressBar(commandBuilder, "#TorsoBar", bodyPartData, BodyPart.TORSO);
                        updateProgressBar(commandBuilder, "#HeadBar", bodyPartData, BodyPart.HEAD);
                        updateProgressBar(commandBuilder, "#RightArmBar", bodyPartData, BodyPart.RIGHTARM);
                        updateProgressBar(commandBuilder, "#LeftArmBar", bodyPartData, BodyPart.LEFTARM);
                        updateProgressBar(commandBuilder, "#RightLegBar", bodyPartData, BodyPart.RIGHTLEG);
                        updateProgressBar(commandBuilder, "#LeftLegBar", bodyPartData, BodyPart.LEFTLEG);

                        updateFractureIcons(commandBuilder, bodyPartData);
                        updateBleedIcons(commandBuilder, bodyPartData);
                        updateHeavyBleedIcons(commandBuilder, bodyPartData);

                        disableButtonsForHealthyParts(commandBuilder, bodyPartData);

                        sendUpdate(commandBuilder);
                    }
                } else {
                    if (refreshTask != null) refreshTask.cancel(false);
                }
            } catch (Exception e) {
                if (refreshTask != null) refreshTask.cancel(false);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void updateBodyPartModel(UICommandBuilder builder, String modelId,
                                     BodyPartComponent bodyPartData, BodyPart part) {
        float current = bodyPartData.getBodyPartHealth(part);
        float max = bodyPartData.getBodyPartMaxHealth(part);

        String color = HealthColorUtil.getHealthColor(current, max);
        builder.set(modelId + ".Background", color);
    }

    private void updateLabel(UICommandBuilder builder, String labelId,
                             BodyPartComponent bodyPartData, BodyPart part) {
        float current = bodyPartData.getBodyPartHealth(part);
        float max = bodyPartData.getBodyPartMaxHealth(part);

        String text = part.getDisplayName() + ": " + String.format("%.0f/%.0f", current, max);
        String color = HealthColorUtil.getHealthColor(current, max);

        builder.set(labelId + ".Text", text);
        builder.set(labelId + ".Style.TextColor", color);
    }

    private void updateProgressBar(UICommandBuilder builder, String barId,
                                   BodyPartComponent bodyPartData, BodyPart part) {
        float current = bodyPartData.getBodyPartHealth(part);
        float max = bodyPartData.getBodyPartMaxHealth(part);

        float percent = (max > 0) ? (current / max) : 0f;

        builder.set(barId + ".Value", Math.max(0f, Math.min(1f, percent)));
    }

    private void updateFractureIcons(UICommandBuilder builder, BodyPartComponent bodyPartData) {
        builder.set("#TorsoFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE"));
        builder.set("#HeadFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "FRACTURE"));
        builder.set("#RightArmFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE"));
        builder.set("#LeftArmFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE"));
        builder.set("#RightLegFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE"));
        builder.set("#LeftLegFractureIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE"));
    }

    private void updateBleedIcons(UICommandBuilder builder, BodyPartComponent bodyPartData) {
        builder.set("#TorsoBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "BLEED"));
        builder.set("#HeadBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "BLEED"));
        builder.set("#RightArmBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "BLEED"));
        builder.set("#LeftArmBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "BLEED"));
        builder.set("#RightLegBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "BLEED"));
        builder.set("#LeftLegBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "BLEED"));
    }

    private void updateHeavyBleedIcons(UICommandBuilder builder, BodyPartComponent bodyPartData) {
        builder.set("#TorsoHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "HEAVY_BLEED"));
        builder.set("#HeadHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "HEAVY_BLEED"));
        builder.set("#RightArmHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "HEAVY_BLEED"));
        builder.set("#LeftArmHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "HEAVY_BLEED"));
        builder.set("#RightLegHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "HEAVY_BLEED"));
        builder.set("#LeftLegHeavyBleedIcon.Visible", bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "HEAVY_BLEED"));
    }

    private boolean consumeSurvivalKit(Player player) {
        ItemContainer[] containers = new ItemContainer[]{
                player.getInventory().getHotbar(),
                player.getInventory().getStorage(),
                player.getInventory().getBackpack()
        };

        for (ItemContainer container : containers) {
            if (container == null) continue;

            for (short slot = 0; slot < container.getCapacity(); slot++) {
                ItemStack stack = container.getItemStack(slot);

                if (stack != null && !stack.isEmpty()
                        && "Survival_Kit_Crude".equals(stack.getItem().getId())) { // ← Changed item ID

                    int quantity = stack.getQuantity();

                    if (quantity > 1) {
                        container.setItemStackForSlot(slot, stack.withQuantity(quantity - 1));
                    } else {
                        container.removeItemStackFromSlot(slot);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        BodyPartComponent data = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (data != null) {
            updateBodyPartModel(cmd, "#HeadModel", data, BodyPart.HEAD);
            updateBodyPartModel(cmd, "#TorsoModel", data, BodyPart.TORSO);
            updateBodyPartModel(cmd, "#RightArmModel", data, BodyPart.RIGHTARM);
            updateBodyPartModel(cmd, "#LeftArmModel", data, BodyPart.LEFTARM);
            updateBodyPartModel(cmd, "#RightLegModel", data, BodyPart.RIGHTLEG);
            updateBodyPartModel(cmd, "#LeftLegModel", data, BodyPart.LEFTLEG);

            updateLabel(cmd, "#HeadHealth", data, BodyPart.HEAD);
            updateLabel(cmd, "#TorsoHealth", data, BodyPart.TORSO);
            updateLabel(cmd, "#RightArmHealth", data, BodyPart.RIGHTARM);
            updateLabel(cmd, "#LeftArmHealth", data, BodyPart.LEFTARM);
            updateLabel(cmd, "#RightLegHealth", data, BodyPart.RIGHTLEG);
            updateLabel(cmd, "#LeftLegHealth", data, BodyPart.LEFTLEG);

            updateProgressBar(cmd, "#TorsoBar", data, BodyPart.TORSO);
            updateProgressBar(cmd, "#HeadBar", data, BodyPart.HEAD);
            updateProgressBar(cmd, "#RightArmBar", data, BodyPart.RIGHTARM);
            updateProgressBar(cmd, "#LeftArmBar", data, BodyPart.LEFTARM);
            updateProgressBar(cmd, "#RightLegBar", data, BodyPart.RIGHTLEG);
            updateProgressBar(cmd, "#LeftLegBar", data, BodyPart.LEFTLEG);

            updateFractureIcons(cmd, data);
            updateBleedIcons(cmd, data);
            updateHeavyBleedIcons(cmd, data);

            // Re-bind buttons after refresh
            bindSurvKitButtons(events, data);

            //Disable buttons after parts are healed
            disableButtonsForHealthyParts(cmd, data);
        }

        sendUpdate(cmd, events, false);
    }

    @Override
    protected void close() {
        super.close();
        SurvKitUIRegistry.unregister(playerRef); // ← Changed registry
        if (refreshTask != null) {
            refreshTask.cancel(false);
        }
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        super.onDismiss(ref, store);
        SurvKitUIRegistry.unregister(playerRef); // ← Changed registry
        if (refreshTask != null) {
            refreshTask.cancel(false);
        }
    }

    public static class SurvKitEventData {
        public String part;

        public static final BuilderCodec<SurvKitEventData> CODEC;

        static {
            CODEC = BuilderCodec.builder(SurvKitEventData.class, SurvKitEventData::new)
                    .append(new KeyedCodec<>("Part", Codec.STRING),
                            (data, value, info) -> data.part = value,
                            (data, info) -> data.part)
                    .add()
                    .build();
        }
    }

    private void disableButtonsForHealthyParts(UICommandBuilder commandBuilder, BodyPartComponent bodyPartData) {
        for (BodyPart part : UI_PARTS) {
            boolean isBroken = bodyPartData.isBodyPartBroken(part); // ← Changed from hasBodyPartEffect
            String buttonId = "#" + getButtonId(part);

            if (!isBroken) {
                commandBuilder.set(buttonId + ".Disabled", true);
            }
        }
    }
}