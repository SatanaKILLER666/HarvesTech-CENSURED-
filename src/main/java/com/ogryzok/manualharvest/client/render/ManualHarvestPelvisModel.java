package com.ogryzok.manualharvest.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ManualHarvestPelvisModel extends ModelBase {
    private static final float Y_OFFSET = -12.0F;

    private final ModelRenderer bbMain;
    private final ModelRenderer root;
    private final ModelRenderer waist;
    private final ModelRenderer body;
    private final ModelRenderer cape;
    private final ModelRenderer head;
    private final ModelRenderer helmet;
    private final ModelRenderer rightArm;
    private final ModelRenderer rightItem;
    private final ModelRenderer leftArm;
    private final ModelRenderer leftItem;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    private final ModelRenderer bone;
    private final ModelRenderer bone2;

    public ManualHarvestPelvisModel() {
        this.textureWidth = 64;
        this.textureHeight = 32;

        this.bbMain = new ModelRenderer(this);
        this.bbMain.setRotationPoint(0.0F, Y_OFFSET, 0.0F);
        this.bbMain.setTextureOffset(1, 2).addBox(-1.575F, 11.375F, -5.725F, 3, 3, 0, 0.0F);

        this.root = new ModelRenderer(this);
        this.root.setRotationPoint(0.0F, Y_OFFSET, 0.0F);

        this.waist = new ModelRenderer(this);
        this.waist.setRotationPoint(0.0F, 12.0F, 0.0F);
        this.root.addChild(this.waist);

        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 12.0F, 0.0F);
        this.waist.addChild(this.body);

        this.cape = new ModelRenderer(this);
        this.cape.setRotationPoint(0.0F, 0.0F, 2.0F);
        this.body.addChild(this.cape);

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.body.addChild(this.head);

        this.helmet = new ModelRenderer(this);
        this.helmet.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.head.addChild(this.helmet);

        this.rightArm = new ModelRenderer(this);
        this.rightArm.setRotationPoint(-5.0F, -2.0F, 0.0F);
        this.body.addChild(this.rightArm);

        this.rightItem = new ModelRenderer(this);
        this.rightItem.setRotationPoint(-1.0F, -7.0F, 1.0F);
        this.rightArm.addChild(this.rightItem);

        this.leftArm = new ModelRenderer(this);
        this.leftArm.setRotationPoint(5.0F, -2.0F, 0.0F);
        this.body.addChild(this.leftArm);

        this.leftItem = new ModelRenderer(this);
        this.leftItem.setRotationPoint(1.0F, -7.0F, 1.0F);
        this.leftArm.addChild(this.leftItem);

        this.rightLeg = new ModelRenderer(this);
        this.rightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
        this.root.addChild(this.rightLeg);

        this.leftLeg = new ModelRenderer(this);
        this.leftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        this.root.addChild(this.leftLeg);

        this.bone = new ModelRenderer(this);
        this.bone.setRotationPoint(0.0F, Y_OFFSET - 8.0F, -2.0F);
        setRotateAngle(this.bone, 0.0F, (float) Math.toRadians(-90.0D), 0.0F);
        this.bone.setTextureOffset(23, 29).addBox(-0.35F, 18.625F, -1.0F, 1, 3, 2, 0.0F);

        this.bone2 = new ModelRenderer(this);
        this.bone2.setRotationPoint(1.45F, 19.975F, -1.0F);
        setRotateAngle(this.bone2,
                (float) Math.toRadians(-1.66728D),
                (float) Math.toRadians(0.2337D),
                (float) Math.toRadians(2.2048D));
        this.bone2.setTextureOffset(22, 31).addBox(-5.95535F, 0.44237F, 0.875F, 5, 1, 1, 0.0F);
        this.bone.addChild(this.bone2);
    }

    private static void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.bbMain.render(scale);
        this.root.render(scale);
        this.bone.render(scale);
    }
}