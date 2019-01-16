package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;

public class ErodeBlendBrush extends Brush{
	
	private BlendBallBrush blendBall;
	private ErodeBrush erode;

	public ErodeBlendBrush() {
		this.blendBall = new BlendBallBrush();
		this.erode = new ErodeBrush();
		this.setName("Erode BlendBall");
	}
	
	@Override
	protected final void arrow(final SnipeData v) {
		this.blendBall.excludeAir = false;
		this.blendBall.setTargetBlock(this.getTargetBlock());
		this.blendBall.arrow(v);
		this.erode.setTargetBlock(this.getTargetBlock());
		this.erode.arrow(v);
	}
	
	@Override
	protected final void powder(final SnipeData v) {
		this.blendBall.excludeAir = false;
		this.blendBall.setTargetBlock(this.getTargetBlock());
		this.blendBall.arrow(v);
		this.erode.setTargetBlock(this.getTargetBlock());
		this.erode.powder(v);
	}
	
	@Override
	public final void parameters(final String[] par, final SnipeData v) {
		this.blendBall.parameters(par, v);
		this.erode.parameters(par, v);
	}
	
	@Override
	public String getPermissionNode() {
		return "voxelsniper.brush.erodeblend";
	}

	@Override
	public void info(Message vm) {
		this.erode.info(vm);
		this.blendBall.info(vm);
	}

}
