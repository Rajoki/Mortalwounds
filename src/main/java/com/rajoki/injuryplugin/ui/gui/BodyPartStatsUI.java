package com.rajoki.injuryplugin.ui.gui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.utils.HealthColorUtil;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Info/updating for /mwstats ui page

public class BodyPartStatsUI extends InteractiveCustomUIPage<BodyPartStatsUI.BodyPartStatsEventData> {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> refreshTask;
    private final PlayerRef playerRef;

    public BodyPartStatsUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, BodyPartStatsEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        BodyPartUIRegistry.register(playerRef, this);

        uiCommandBuilder.append("BodyPartStats.ui");

        BodyPartComponent bodyPartData = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartData != null) {
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


            // update body part health bars
            updateProgressBar(uiCommandBuilder, "#TorsoBar", bodyPartData, BodyPart.TORSO);
            updateProgressBar(uiCommandBuilder, "#HeadBar", bodyPartData, BodyPart.HEAD);
            updateProgressBar(uiCommandBuilder, "#RightArmBar", bodyPartData, BodyPart.RIGHTARM);
            updateProgressBar(uiCommandBuilder, "#LeftArmBar", bodyPartData, BodyPart.LEFTARM);
            updateProgressBar(uiCommandBuilder, "#RightLegBar", bodyPartData, BodyPart.RIGHTLEG);
            updateProgressBar(uiCommandBuilder, "#LeftLegBar", bodyPartData, BodyPart.LEFTLEG);

            // Update Fracture icons
            boolean torsoFractured = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE");
            boolean headFractured = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "FRACTURE");
            boolean rightArmFractured = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE");
            boolean leftArmFractured = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE");
            boolean rightLegFractured = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE");
            boolean leftLegFractured = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE");

            uiCommandBuilder.set("#TorsoFractureIcon.Visible", torsoFractured);
            uiCommandBuilder.set("#HeadFractureIcon.Visible", headFractured);
            uiCommandBuilder.set("#RightArmFractureIcon.Visible", rightArmFractured);
            uiCommandBuilder.set("#LeftArmFractureIcon.Visible", leftArmFractured);
            uiCommandBuilder.set("#RightLegFractureIcon.Visible", rightLegFractured);
            uiCommandBuilder.set("#LeftLegFractureIcon.Visible", leftLegFractured);

            //Update Bleed icons
            boolean torsoBleeding = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "BLEED");
            boolean headBleeding = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "BLEED");
            boolean rightArmBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "BLEED");
            boolean leftArmBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "BLEED");
            boolean rightLegBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "BLEED");
            boolean leftLegBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "BLEED");

            uiCommandBuilder.set("#TorsoBleedIcon.Visible", torsoBleeding);
            uiCommandBuilder.set("#HeadBleedIcon.Visible", headBleeding);
            uiCommandBuilder.set("#RightArmBleedIcon.Visible", rightArmBleeding);
            uiCommandBuilder.set("#LeftArmBleedIcon.Visible", leftArmBleeding);
            uiCommandBuilder.set("#RightLegBleedIcon.Visible", rightLegBleeding);
            uiCommandBuilder.set("#LeftLegBleedIcon.Visible", leftLegBleeding);

            //Heavy Bleed Icons
            boolean torsoHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "HEAVY_BLEED");
            boolean headHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "HEAVY_BLEED");
            boolean rightArmHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "HEAVY_BLEED");
            boolean leftArmHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "HEAVY_BLEED");
            boolean rightLegHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "HEAVY_BLEED");
            boolean leftLegHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "HEAVY_BLEED");

            uiCommandBuilder.set("#TorsoHeavyBleedIcon.Visible", torsoHeavyBleeding);
            uiCommandBuilder.set("#HeadHeavyBleedIcon.Visible", headHeavyBleeding);
            uiCommandBuilder.set("#RightArmHeavyBleedIcon.Visible", rightArmHeavyBleeding);
            uiCommandBuilder.set("#LeftArmHeavyBleedIcon.Visible", leftArmHeavyBleeding);
            uiCommandBuilder.set("#RightLegHeavyBleedIcon.Visible", rightLegHeavyBleeding);
            uiCommandBuilder.set("#LeftLegHeavyBleedIcon.Visible", leftLegHeavyBleeding);




        }

        startAutoRefresh(ref, store);
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


                        // update body part health bars
                        updateProgressBar(commandBuilder, "#TorsoBar", bodyPartData, BodyPart.TORSO);
                        updateProgressBar(commandBuilder, "#HeadBar", bodyPartData, BodyPart.HEAD);
                        updateProgressBar(commandBuilder, "#RightArmBar", bodyPartData, BodyPart.RIGHTARM);
                        updateProgressBar(commandBuilder, "#LeftArmBar", bodyPartData, BodyPart.LEFTARM);
                        updateProgressBar(commandBuilder, "#RightLegBar", bodyPartData, BodyPart.RIGHTLEG);
                        updateProgressBar(commandBuilder, "#LeftLegBar", bodyPartData, BodyPart.LEFTLEG);
//update Body Part Icons


                        boolean torsoFractured = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE");
                        boolean headFractured = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "FRACTURE");
                        boolean rightArmFractured = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE");
                        boolean leftArmFractured = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE");
                        boolean rightLegFractured = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE");
                        boolean leftLegFractured = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE");

                        commandBuilder.set("#TorsoFractureIcon.Visible", torsoFractured);
                        commandBuilder.set("#HeadFractureIcon.Visible", headFractured);
                        commandBuilder.set("#RightArmFractureIcon.Visible", rightArmFractured);
                        commandBuilder.set("#LeftArmFractureIcon.Visible", leftArmFractured);
                        commandBuilder.set("#RightLegFractureIcon.Visible", rightLegFractured);
                        commandBuilder.set("#LeftLegFractureIcon.Visible", leftLegFractured);


                        boolean torsoBleeding = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "BLEED");
                        boolean headBleeding = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "BLEED");
                        boolean rightArmBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "BLEED");
                        boolean leftArmBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "BLEED");
                        boolean rightLegBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "BLEED");
                        boolean leftLegBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "BLEED");

                        commandBuilder.set("#TorsoBleedIcon.Visible", torsoBleeding);
                        commandBuilder.set("#HeadBleedIcon.Visible", headBleeding);
                        commandBuilder.set("#RightArmBleedIcon.Visible", rightArmBleeding);
                        commandBuilder.set("#LeftArmBleedIcon.Visible", leftArmBleeding);
                        commandBuilder.set("#RightLegBleedIcon.Visible", rightLegBleeding);
                        commandBuilder.set("#LeftLegBleedIcon.Visible", leftLegBleeding);

                        //Heavy bleed
                        boolean torsoHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.TORSO, "HEAVY_BLEED");
                        boolean headHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.HEAD, "HEAVY_BLEED");
                        boolean rightArmHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTARM, "HEAVY_BLEED");
                        boolean leftArmHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTARM, "HEAVY_BLEED");
                        boolean rightLegHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.RIGHTLEG, "HEAVY_BLEED");
                        boolean leftLegHeavyBleeding = bodyPartData.hasBodyPartEffect(BodyPart.LEFTLEG, "HEAVY_BLEED");

                        commandBuilder.set("#TorsoHeavyBleedIcon.Visible", torsoHeavyBleeding);
                        commandBuilder.set("#HeadHeavyBleedIcon.Visible", headHeavyBleeding);
                        commandBuilder.set("#RightArmHeavyBleedIcon.Visible", rightArmHeavyBleeding);
                        commandBuilder.set("#LeftArmHeavyBleedIcon.Visible", leftArmHeavyBleeding);
                        commandBuilder.set("#RightLegHeavyBleedIcon.Visible", rightLegHeavyBleeding);
                        commandBuilder.set("#LeftLegHeavyBleedIcon.Visible", leftLegHeavyBleeding);







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

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull BodyPartStatsEventData data) {

        super.handleDataEvent(ref, store, data);

        // Just acknowledge event
        sendUpdate();
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
        boolean hasFracture = bodyPartData.hasBodyPartEffect(part, "FRACTURE");
        boolean hasBleed = bodyPartData.hasBodyPartEffect(part, "BLEED");
        boolean hasHeavyBleed = bodyPartData.hasBodyPartEffect(part, "HEAVY_BLEED");

        String text = part.getDisplayName() + ": " + String.format("%.0f/%.0f", current, max);
        String color = HealthColorUtil.getHealthColor(current, max);

        builder.set(labelId + ".Text", text);
        builder.set(labelId + ".Style.TextColor", color);

//        // Update fracture/bleed label with Heavy Bleed support
//        String fractureLabelId = labelId.replace("Health", "Fracture");
//
//        if (hasFracture && hasHeavyBleed) {
//            builder.set(fractureLabelId + ".Text", "FRACTURED | HEAVY BLEEDING");
//            builder.set(fractureLabelId + ".Style.TextColor", "#DC143C"); // Crimson red
//        } else if (hasFracture && hasBleed) {
//            builder.set(fractureLabelId + ".Text", "FRACTURED | BLEEDING");
//            builder.set(fractureLabelId + ".Style.TextColor", "#FF4500");
//        } else if (hasHeavyBleed) {
//            builder.set(fractureLabelId + ".Text", "HEAVY BLEEDING");
//            builder.set(fractureLabelId + ".Style.TextColor", "#DC143C"); // Crimson red
//        } else if (hasFracture) {
//            builder.set(fractureLabelId + ".Text", "FRACTURED");
//            builder.set(fractureLabelId + ".Style.TextColor", "#FFA500");
//        } else if (hasBleed) {
//            builder.set(fractureLabelId + ".Text", "BLEEDING");
//            builder.set(fractureLabelId + ".Style.TextColor", "#FF0000");
//        } else {
//            builder.set(fractureLabelId + ".Text", "");
//        }
    }

    private void updateProgressBar(UICommandBuilder builder, String barId,
                                   BodyPartComponent bodyPartData, BodyPart part) {
        float current = bodyPartData.getBodyPartHealth(part);
        float max = bodyPartData.getBodyPartMaxHealth(part);

        float percent = (max > 0) ? (current / max) : 0f;

        builder.set(barId + ".Value", Math.max(0f, Math.min(1f, percent)));
    }

//    private String getHealthColor(float current, float max) {
//        float percent = (max > 0) ? (current / max) * 100f : 100f;
//
//        if (percent <= 0) return "#000000";      // Black - destroyed
//        if (percent <= 25) return "#FF0000";     // Red
//        if (percent <= 50) return "#FFA500";     // Orange
//        if (percent <= 75) return "#FFFF00";     // Yellow
//        if (percent <= 90) return "#5aff08";     // Light green
//        return "#00FF00";                        // Green
//    }

    public static final class BodyPartStatsEventData {
        public static final BuilderCodec<BodyPartStatsEventData> CODEC =
                BuilderCodec.builder(BodyPartStatsEventData.class, BodyPartStatsEventData::new).build();
    }

    @Override
    protected void close() {
        super.close();
        BodyPartUIRegistry.unregister(playerRef);
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        super.onDismiss(ref, store);
        BodyPartUIRegistry.unregister(playerRef);
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

            // update body part health bars
            updateProgressBar(cmd, "#TorsoBar", data, BodyPart.TORSO);
            updateProgressBar(cmd, "#HeadBar", data, BodyPart.HEAD);
            updateProgressBar(cmd, "#RightArmBar", data, BodyPart.RIGHTARM);
            updateProgressBar(cmd, "#LeftArmBar", data, BodyPart.LEFTARM);
            updateProgressBar(cmd, "#RightLegBar", data, BodyPart.RIGHTLEG);
            updateProgressBar(cmd, "#LeftLegBar", data, BodyPart.LEFTLEG);

            // Update Status Effect icons
            //Fracture Icons
            boolean torsoFractured = data.hasBodyPartEffect(BodyPart.TORSO, "FRACTURE");
            boolean headFractured = data.hasBodyPartEffect(BodyPart.HEAD, "FRACTURE");
            boolean rightArmFractured = data.hasBodyPartEffect(BodyPart.RIGHTARM, "FRACTURE");
            boolean leftArmFractured = data.hasBodyPartEffect(BodyPart.LEFTARM, "FRACTURE");
            boolean rightLegFractured = data.hasBodyPartEffect(BodyPart.RIGHTLEG, "FRACTURE");
            boolean leftLegFractured = data.hasBodyPartEffect(BodyPart.LEFTLEG, "FRACTURE");

            cmd.set("#TorsoFractureIcon.Visible", torsoFractured);
            cmd.set("#HeadFractureIcon.Visible", headFractured);
            cmd.set("#RightArmFractureIcon.Visible", rightArmFractured);
            cmd.set("#LeftArmFractureIcon.Visible", leftArmFractured);
            cmd.set("#RightLegFractureIcon.Visible", rightLegFractured);
            cmd.set("#LeftLegFractureIcon.Visible", leftLegFractured);

            //Bleed icons

            boolean torsoBleeding = data.hasBodyPartEffect(BodyPart.TORSO, "BLEED");
            boolean headBleeding = data.hasBodyPartEffect(BodyPart.HEAD, "BLEED");
            boolean rightArmBleeding = data.hasBodyPartEffect(BodyPart.RIGHTARM, "BLEED");
            boolean leftArmBleeding = data.hasBodyPartEffect(BodyPart.LEFTARM, "BLEED");
            boolean rightLegBleeding = data.hasBodyPartEffect(BodyPart.RIGHTLEG, "BLEED");
            boolean leftLegBleeding = data.hasBodyPartEffect(BodyPart.LEFTLEG, "BLEED");

            cmd.set("#TorsoBleedIcon.Visible", torsoBleeding);
            cmd.set("#HeadBleedIcon.Visible", headBleeding);
            cmd.set("#RightArmBleedIcon.Visible", rightArmBleeding);
            cmd.set("#LeftArmBleedIcon.Visible", leftArmBleeding);
            cmd.set("#RightLegBleedIcon.Visible", rightLegBleeding);
            cmd.set("#LeftLegBleedIcon.Visible", leftLegBleeding);

            //Heavy bleed

            boolean torsoHeavyBleeding = data.hasBodyPartEffect(BodyPart.TORSO, "HEAVY_BLEED");
            boolean headHeavyBleeding = data.hasBodyPartEffect(BodyPart.HEAD, "HEAVY_BLEED");
            boolean rightArmHeavyBleeding = data.hasBodyPartEffect(BodyPart.RIGHTARM, "HEAVY_BLEED");
            boolean leftArmHeavyBleeding = data.hasBodyPartEffect(BodyPart.LEFTARM, "HEAVY_BLEED");
            boolean rightLegHeavyBleeding = data.hasBodyPartEffect(BodyPart.RIGHTLEG, "HEAVY_BLEED");
            boolean leftLegHeavyBleeding = data.hasBodyPartEffect(BodyPart.LEFTLEG, "HEAVY_BLEED");

            cmd.set("#TorsoHeavyBleedIcon.Visible", torsoHeavyBleeding);
            cmd.set("#HeadHeavyBleedIcon.Visible", headHeavyBleeding);
            cmd.set("#RightArmHeavyBleedIcon.Visible", rightArmHeavyBleeding);
            cmd.set("#LeftArmHeavyBleedIcon.Visible", leftArmHeavyBleeding);
            cmd.set("#RightLegHeavyBleedIcon.Visible", rightLegHeavyBleeding);
            cmd.set("#LeftLegHeavyBleedIcon.Visible", leftLegHeavyBleeding);



        }

        sendUpdate(cmd, events, false);

    }



}