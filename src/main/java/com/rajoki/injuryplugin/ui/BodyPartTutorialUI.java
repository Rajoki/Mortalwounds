package com.rajoki.injuryplugin.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.rajoki.injuryplugin.MortalWoundsPlugin;
import com.rajoki.injuryplugin.components.BodyPartComponent;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.utils.HealthColorUtil;

import javax.annotation.Nonnull;

public class BodyPartTutorialUI extends InteractiveCustomUIPage<BodyPartTutorialUI.TutorialEventData> {

    private final PlayerRef playerRef;
    private int currentPage = 0;

    public BodyPartTutorialUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, TutorialEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        uiCommandBuilder.append("BodyPartTutorial.ui");

        // Get player's actual body part data
        BodyPartComponent bodyPartData = store.getComponent(ref,
                MortalWoundsPlugin.getInstance().getBodyPartComponentType());

        if (bodyPartData != null) {
            // Update body part visuals with player's actual health
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
        }

        // Set initial tutorial text
        updateTutorialText(uiCommandBuilder);

        // Bind button events
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "back"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NextButton", EventData.of("Action", "next"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull TutorialEventData data) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        switch (data.action) {
            case "next":
                if (currentPage < TutorialPageData.getTotalPages() - 1) {
                    currentPage++;
                    refresh(ref, store);
                }
                break;
            case "back":
                if (currentPage > 0) {
                    currentPage--;
                    refresh(ref, store);
                }
                break;
            case "close":
                player.getPageManager().setPage(ref, store, Page.None);
                break;
        }
    }

    private void updateTutorialText(UICommandBuilder builder) {
        // Update text
        String pageText = TutorialPageData.getPageText(currentPage);
        builder.set("#TutorialText.Text", pageText);

        // Update page counter
        String counterText = String.format("%d / %d", currentPage + 1, TutorialPageData.getTotalPages());
        builder.set("#PageCounter.Text", counterText);

        // Update button states
        builder.set("#BackButton.Disabled", currentPage == 0);
        builder.set("#NextButton.Disabled", currentPage == TutorialPageData.getTotalPages() - 1);

        // Change Next to "Finish" on last page
        if (currentPage == TutorialPageData.getTotalPages() - 1) {
            builder.set("#NextButton.Text", "Finish");
        } else {
            builder.set("#NextButton.Text", "Next");
        }
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

    public void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

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
        }

        updateTutorialText(cmd);

        sendUpdate(cmd);
    }

    public static class TutorialEventData {
        public String action;

        public static final BuilderCodec<TutorialEventData> CODEC;

        static {
            CODEC = BuilderCodec.builder(TutorialEventData.class, TutorialEventData::new)
                    .append(new KeyedCodec<>("Action", Codec.STRING),
                            (data, value, info) -> data.action = value,
                            (data, info) -> data.action)
                    .add()
                    .build();
        }
    }
}