package com.rajoki.injuryplugin.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.rajoki.injuryplugin.utils.HealthColorUtil;
import com.rajoki.injuryplugin.systems.bodypartsystems.BodyPart;
import com.rajoki.injuryplugin.components.BodyPartComponent;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.WeakHashMap;

// Body part HUD that is always displayed on screen

public class BodyPartHud extends CustomUIHud {

    // Static registry like EasyHunger
    private static final WeakHashMap<UUID, BodyPartHud> hudMap = new WeakHashMap<>();

    private BodyPartComponent bodyPartData;
    private boolean built = false;

    public BodyPartHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
        // Register in map when created
        hudMap.put(playerRef.getUuid(), this);
    }

    public void setBodyPartData(BodyPartComponent bodyPartData) {
        this.bodyPartData = bodyPartData;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {



            uiCommandBuilder.append("Hud/BodyPartHud.ui");
        built = true;


        if (bodyPartData != null && bodyPartData.isInitialized()) {
            updateAllBodyParts(uiCommandBuilder);
        } else {
            setDefaultColors(uiCommandBuilder);
            clearStatusLabels(uiCommandBuilder);
            hideAllIcons(uiCommandBuilder);
        }
    }

    private void updateAllBodyParts(UICommandBuilder builder) {
        updateBodyPart(builder, "#HeadModel", "#HeadStatus", BodyPart.HEAD);
        updateBodyPart(builder, "#TorsoModel", "#TorsoStatus", BodyPart.TORSO);
        updateBodyPart(builder, "#LeftArmModel", "#LeftArmStatus", BodyPart.LEFTARM);
        updateBodyPart(builder, "#RightArmModel", "#RightArmStatus", BodyPart.RIGHTARM);
        updateBodyPart(builder, "#LeftLegModel", "#LeftLegStatus", BodyPart.LEFTLEG);
        updateBodyPart(builder, "#RightLegModel", "#RightLegStatus", BodyPart.RIGHTLEG);
    }

    private void updateBodyPart(UICommandBuilder builder, String modelId, String statusId, BodyPart part) {
        float current = bodyPartData.getBodyPartHealth(part);
        float max = bodyPartData.getBodyPartMaxHealth(part);

        boolean hasBleed = bodyPartData.hasBodyPartEffect(part, "BLEED");
        boolean hasHeavyBleed = bodyPartData.hasBodyPartEffect(part, "HEAVY_BLEED");
        boolean hasFracture = bodyPartData.hasBodyPartEffect(part, "FRACTURE");

        // Model color
        builder.set(modelId + ".Background", HealthColorUtil.getHealthColor(current, max));

        // Optional — keep or remove text
        builder.set(statusId + ".Text", "");

        // Build icon prefix from part
        String prefix = "#" + getPartPrefix(part);

        // Toggle icons
        builder.set(prefix + "FractureIcon.Visible", hasFracture);
        builder.set(prefix + "BleedIcon.Visible", hasBleed);
        builder.set(prefix + "HeavyBleedIcon.Visible", hasHeavyBleed);
    }

    private String getPartPrefix(BodyPart part) {
        switch (part) {
            case HEAD: return "Head";
            case TORSO: return "Torso";
            case LEFTARM: return "LeftArm";
            case RIGHTARM: return "RightArm";
            case LEFTLEG: return "LeftLeg";
            case RIGHTLEG: return "RightLeg";
            default: return "";
        }
    }

    private void setDefaultColors(UICommandBuilder builder) {
        builder.set("#HeadModel.Background", "#00FF00");
        builder.set("#TorsoModel.Background", "#00FF00");
        builder.set("#LeftArmModel.Background", "#00FF00");
        builder.set("#RightArmModel.Background", "#00FF00");
        builder.set("#LeftLegModel.Background", "#00FF00");
        builder.set("#RightLegModel.Background", "#00FF00");
    }

    private void clearStatusLabels(UICommandBuilder builder) {
        builder.set("#HeadStatus.Text", "");
        builder.set("#TorsoStatus.Text", "");
        builder.set("#LeftArmStatus.Text", "");
        builder.set("#RightArmStatus.Text", "");
        builder.set("#LeftLegStatus.Text", "");
        builder.set("#RightLegStatus.Text", "");
    }

    private void hideAllIcons(UICommandBuilder builder) {
        String[] parts = {"Head", "Torso", "LeftArm", "RightArm", "LeftLeg", "RightLeg"};

        for (String p : parts) {
            builder.set("#" + p + "FractureIcon.Visible", false);
            builder.set("#" + p + "BleedIcon.Visible", false);
            builder.set("#" + p + "HeavyBleedIcon.Visible", false);
        }
    }

    // Keep this for backward compatibility, but prefer the static method
    public void refresh(BodyPartComponent bodyPartData) {
        this.bodyPartData = bodyPartData;

        UICommandBuilder builder = new UICommandBuilder();

        if (bodyPartData != null && bodyPartData.isInitialized()) {
            updateAllBodyParts(builder);
        }

        this.update(false, builder);
    }

    public static void updatePlayerBodyParts(@Nonnull UUID playerUuid,
                                             @Nonnull BodyPartComponent bodyPartData) {

        //System.out.println("[DEBUG BodyPartHud] updatePlayerBodyParts called for player: " + playerUuid);
        BodyPartHud hud = hudMap.get(playerUuid);

        if (hud == null) {
            //System.out.println("[DEBUG BodyPartHud] HUD NOT found in map!");
            return;
        }

        // 🚨 IMPORTANT CHECK
        if (!hud.built) {
            //System.out.println("[DEBUG BodyPartHud] HUD not built yet, skipping update...");
            return;
        }

        //System.out.println("[DEBUG BodyPartHud] HUD found in map, updating...");

        hud.bodyPartData = bodyPartData;

        UICommandBuilder builder = new UICommandBuilder();
        hud.updateAllBodyParts(builder);

        //  DO NOT rebuild during normal updates
        hud.update(false, builder);

       //System.out.println("[DEBUG BodyPartHud] Update sent!");
    }

    // For cleanup
    public static void removeFromRegistry(UUID playerUuid) {
        hudMap.remove(playerUuid);
    }
}