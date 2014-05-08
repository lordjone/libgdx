package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;

public class LoggerInfluencer extends Influencer {

	@Override
	public void update () {
		//System.out.println("particles count "+controller.particleChannels.size);
	}
	
	@Override
	public void start () {
		System.out.println("start");
	}
	
	@Override
	public void activateParticles (int startIndex, int count) {
		//System.out.println("activated particles "+count);
	}
	
	@Override
	public void killParticles (int startIndex, int count) {
		//System.out.println("killed particles "+count);
	}
	
	
	@Override
	public ParticleControllerComponent copy () {
		return new LoggerInfluencer();
	}

}
