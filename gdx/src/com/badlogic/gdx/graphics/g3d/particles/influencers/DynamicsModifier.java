package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/** It's the base class for any kind of influencer which operates on angular velocity and acceleration of the particles.
 * All the classes that will inherit this base class can and should be used only as sub-influencer inside an instance of {@link DynamicsInfluencer} .
 *  @author Inferno */
public abstract class DynamicsModifier extends Influencer{
	protected static final Vector3 	TMP_V1 = new Vector3(), 
		 										TMP_V2 = new Vector3(), 
		 										TMP_V3 = new Vector3();
	protected static final Quaternion TMP_Q = new Quaternion();
	
	public static class FaceDirection extends DynamicsModifier {
		FloatChannel rotationChannel, accellerationChannel;
		
		public FaceDirection(){}
		
		public FaceDirection (FaceDirection rotation) {
			super(rotation);
		}
		@Override
		public void allocateChannels() {
			rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D);
			accellerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration);
		}
		
		@Override
		public void update () {
			for(int 	i=0, accelOffset = 0, c = i +controller.particles.size *rotationChannel.strideSize; 
				i < c; 
				i +=rotationChannel.strideSize, accelOffset += accellerationChannel.strideSize){
				
				Vector3 	axisZ = TMP_V1.set(	accellerationChannel.data[accelOffset +ParticleChannels.XOffset], 
																				accellerationChannel.data[accelOffset +ParticleChannels.YOffset], 
																				accellerationChannel.data[accelOffset +ParticleChannels.ZOffset]).nor(),
					axisY = TMP_V2.set(TMP_V1).crs(Vector3.Y).nor().crs(TMP_V1).nor(),
					axisX = TMP_V3.set(axisY).crs(axisZ).nor();
				TMP_Q.setFromAxes(false, 	axisX.x,  axisY.x, axisZ.x,
															axisX.y,  axisY.y, axisZ.y,
															axisX.z,  axisY.z, axisZ.z);
				rotationChannel.data[i +ParticleChannels.XOffset] = TMP_Q.x;
				rotationChannel.data[i +ParticleChannels.YOffset] = TMP_Q.y;
				rotationChannel.data[i +ParticleChannels.ZOffset] = TMP_Q.z;
				rotationChannel.data[i +ParticleChannels.WOffset] = TMP_Q.w;	
			}
		}

		@Override
		public ParticleControllerComponent copy () {
			return new FaceDirection(this);
		}
	}
	
	public static abstract class Strength extends DynamicsModifier {
		protected FloatChannel strengthChannel;
		public ScaledNumericValue strengthValue;
		
		public Strength(){
			strengthValue = new ScaledNumericValue();
		}
		
		public Strength (Strength rotation) {
			super(rotation);
			strengthValue = new ScaledNumericValue();
			strengthValue.load(rotation.strengthValue);
		}
		
		@Override
		public void allocateChannels() {
			super.allocateChannels();
			ParticleChannels.Interpolation.id = controller.particleChannels.newId();
			strengthChannel = controller.particles.addChannel(ParticleChannels.Interpolation);			
		}
		
		@Override
		public void activateParticles (int startIndex, int count) {
			float start, diff;
			for(int 	i=startIndex*strengthChannel.strideSize, c = i +count*strengthChannel.strideSize; 
				i < c; 
				i +=strengthChannel.strideSize){
				start = strengthValue.newLowValue(); 
				diff = strengthValue.newHighValue();
				if(!strengthValue.isRelative())
					diff -= start;
				strengthChannel.data[i + ParticleChannels.VelocityStrengthStartOffset] = start;
				strengthChannel.data[i + ParticleChannels.VelocityStrengthDiffOffset] = diff;
			}
		}

		@Override
		public void write (Json json) {
			super.write(json);
			json.writeValue("strengthValue", strengthValue);
		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			super.read(json, jsonData);
			strengthValue = json.readValue("strengthValue", ScaledNumericValue.class, jsonData);
		}
	}
	
	public static abstract class Angular extends Strength {
		protected FloatChannel angularChannel;
		/** Polar angle, XZ plane */
		public ScaledNumericValue thetaValue;
		/** Azimuth, Y */
		public ScaledNumericValue phiValue;

		public Angular(){
			thetaValue = new ScaledNumericValue();
			phiValue = new ScaledNumericValue();
		}
		
		public Angular (Angular value) {
			super(value);
			thetaValue = new ScaledNumericValue();
			phiValue = new ScaledNumericValue();
			thetaValue.load(value.thetaValue);
			phiValue.load(value.phiValue);
		}
		
		@Override
		public void allocateChannels() {
			super.allocateChannels();
			ParticleChannels.Interpolation4.id = controller.particleChannels.newId();
			angularChannel = controller.particles.addChannel(ParticleChannels.Interpolation4);			
		}
		
		@Override
		public void activateParticles (int startIndex, int count) {
			super.activateParticles(startIndex, count);
			float start, diff;
			for(int 	i=startIndex*angularChannel.strideSize, c = i +count*angularChannel.strideSize; 
				i < c; 
				i +=angularChannel.strideSize){
				
				//Theta
				start = thetaValue.newLowValue();
				diff = thetaValue.newHighValue();
				if(!thetaValue.isRelative())
					diff -= start;
				angularChannel.data[i + ParticleChannels.VelocityThetaStartOffset] = start;
				angularChannel.data[i + ParticleChannels.VelocityThetaDiffOffset] = diff;

				//Phi
				start = phiValue.newLowValue();
				diff = phiValue.newHighValue();
				if(!phiValue.isRelative())
					diff -= start;
				angularChannel.data[i + ParticleChannels.VelocityPhiStartOffset] = start;
				angularChannel.data[i + ParticleChannels.VelocityPhiDiffOffset] = diff;
			}
		}
		
		@Override
		public void write (Json json) {
			super.write(json);
			json.writeValue("thetaValue", thetaValue);
			json.writeValue("phiValue", phiValue);
		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			super.read(json, jsonData);
			thetaValue = json.readValue("thetaValue", ScaledNumericValue.class, jsonData);
			phiValue = json.readValue("phiValue", ScaledNumericValue.class, jsonData);
		}
	}
	
	
	public static class Rotational2D extends Strength {
		FloatChannel rotationalVelocity2dChannel;
		
		public Rotational2D (){}
		
		public Rotational2D (Rotational2D rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			rotationalVelocity2dChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity2D);
		}
		
		@Override
		public void update () {
			for(int 	i=0, l = ParticleChannels.LifePercentOffset, s =0,
				c = i +controller.particles.size*rotationalVelocity2dChannel.strideSize; 
				i < c; 
				s += strengthChannel.strideSize, i +=rotationalVelocity2dChannel.strideSize,  l +=lifeChannel.strideSize){
				rotationalVelocity2dChannel.data[i] += 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
																									strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifeChannel.data[l]);
			}
		}

		@Override
		public Rotational2D copy () {
			return new Rotational2D(this);
		}
	}
	
	public static class Rotational3D extends Angular {
		FloatChannel rotationChannel, rotationalForceChannel;
		
		public Rotational3D(){}
		
		public Rotational3D (Rotational3D rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D);
			rotationalForceChannel = controller.particles.addChannel(ParticleChannels.AngularVelocity3D);
		}
		
		@Override
		public void update () {
			
			//Matrix3 I_t = defined by the shape, it's the inertia tensor
			//Vector3 r = position vector
			//Vector3 L = r.cross(v.mul(m)), It's the angular momentum, where mv it's the linear momentum
			//Inverse(I_t) = a diagonal matrix where the diagonal is IyIz, IxIz, IxIy
			//Vector3 w = L/I_t = inverse(I_t)*L, It's the angular velocity
			//Quaternion spin = 0.5f*Quaternion(w, 0)*currentRotation
			//currentRotation += spin*dt
			//normalize(currentRotation)
		
			//Algorithm 1
			//Consider a simple channel which represent an angular velocity w
			//Sum each w for each rotation 
			//Update rotation
			
			//Algorithm 2
			//Consider a channel which represent a sort of angular momentum L  (r, v)
			//Sum each L for each rotation 
			//Multiply sum by constant quantity k = m*Iö-1 , m could be optional while I is constant and can be calculated at start 
			//Update rotation			
			
			//Algorithm 3
			//Consider a channel which represent a simple angular momentum L
			//Proceed as Algorithm 2
			
			for(int 	i=0, l = ParticleChannels.LifePercentOffset, s =0, a = 0,
				c = controller.particles.size*rotationalForceChannel.strideSize; 
				i < c; 
				s += strengthChannel.strideSize, i +=rotationalForceChannel.strideSize, 
				a += angularChannel.strideSize, l += lifeChannel.strideSize){
				
				float 	lifePercent = lifeChannel.data[l],
							strength = 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifePercent),
							phi = 	angularChannel.data[a + ParticleChannels.VelocityPhiStartOffset] + 
											angularChannel.data[a + ParticleChannels.VelocityPhiDiffOffset]* phiValue.getScale(lifePercent),
							theta = 	angularChannel.data[a + ParticleChannels.VelocityThetaStartOffset] + 
												angularChannel.data[a + ParticleChannels.VelocityThetaDiffOffset]* thetaValue.getScale(lifePercent);
				
				float 	cosTheta = MathUtils.cosDeg(theta), sinTheta = MathUtils.sinDeg(theta),
							cosPhi = MathUtils.cosDeg(phi), sinPhi = MathUtils.sinDeg(phi);
				
				TMP_V3.set(cosTheta *sinPhi, cosPhi, sinTheta*sinPhi);
				TMP_V3.scl(strength*MathUtils.degreesToRadians);
				
				rotationalForceChannel.data[i +ParticleChannels.XOffset] += TMP_V3.x;
				rotationalForceChannel.data[i +ParticleChannels.YOffset] += TMP_V3.y;
				rotationalForceChannel.data[i +ParticleChannels.ZOffset] += TMP_V3.z;				
			}
		}

		@Override
		public Rotational3D copy () {
			return new Rotational3D(this);
		}
	}
	
	
	public static class CentripetalAcceleration extends Strength {
		FloatChannel accelerationChannel;
		FloatChannel positionChannel;
		public CentripetalAcceleration(){}
		
		public CentripetalAcceleration (CentripetalAcceleration rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration);
			positionChannel = controller.particles.addChannel(ParticleChannels.Position);
		}
		
		@Override
		public void update () {
			float cx = 0, cy = 0, cz = 0;
			if(!isGlobal){
				float[] val = controller.transform.val;
				cx = val[Matrix4.M03]; 
				cy = val[Matrix4.M13]; 
				cz = val[Matrix4.M23];
			}
			
			int lifeOffset=ParticleChannels.LifePercentOffset, strengthOffset = 0, positionOffset = 0, forceOffset = 0;
			for(int 	i=0,  c= controller.particles.size; i < c; ++i,  
				positionOffset += positionChannel.strideSize,
				strengthOffset += strengthChannel.strideSize, 
				forceOffset +=accelerationChannel.strideSize, 
				lifeOffset += lifeChannel.strideSize){
			
				float 	strength = 	strengthChannel.data[strengthOffset + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[strengthOffset + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifeChannel.data[lifeOffset]);
				TMP_V3.set(	positionChannel.data[positionOffset +ParticleChannels.XOffset] -cx, 
											positionChannel.data[positionOffset +ParticleChannels.YOffset] -cy, 
											positionChannel.data[positionOffset +ParticleChannels.ZOffset] -cz)
								.nor().scl(strength);
				accelerationChannel.data[forceOffset +ParticleChannels.XOffset] += TMP_V3.x;
				accelerationChannel.data[forceOffset +ParticleChannels.YOffset] += TMP_V3.y;
				accelerationChannel.data[forceOffset +ParticleChannels.ZOffset] += TMP_V3.z;
			}
		}

		@Override
		public CentripetalAcceleration copy () {
			return new CentripetalAcceleration(this);
		}
	}
	
	public static class PolarAcceleration extends Angular {
		FloatChannel directionalVelocityChannel;
		public PolarAcceleration(){}
		
		public PolarAcceleration (PolarAcceleration rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration);
		}
		
		@Override
		public void update () {
			for(int 	i=0, l = ParticleChannels.LifePercentOffset, s =0, a = 0,
				c = i +controller.particles.size*directionalVelocityChannel.strideSize; 
				i < c; 
				s += strengthChannel.strideSize, i +=directionalVelocityChannel.strideSize, 
				a += angularChannel.strideSize, l += lifeChannel.strideSize){
				
				float 	lifePercent = lifeChannel.data[l],
							strength = 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifePercent),
							phi = 	angularChannel.data[a + ParticleChannels.VelocityPhiStartOffset] + 
											angularChannel.data[a + ParticleChannels.VelocityPhiDiffOffset]* phiValue.getScale(lifePercent),
							theta = 	angularChannel.data[a + ParticleChannels.VelocityThetaStartOffset] + 
												angularChannel.data[a + ParticleChannels.VelocityThetaDiffOffset]* thetaValue.getScale(lifePercent);
				
				float cosTheta = MathUtils.cosDeg(theta), sinTheta = MathUtils.sinDeg(theta),
					cosPhi = MathUtils.cosDeg(phi), sinPhi = MathUtils.sinDeg(phi);
				TMP_V3.set(cosTheta *sinPhi, cosPhi, sinTheta*sinPhi).nor().scl(strength);	
				directionalVelocityChannel.data[i +ParticleChannels.XOffset] += TMP_V3.x;
				directionalVelocityChannel.data[i +ParticleChannels.YOffset] += TMP_V3.y;
				directionalVelocityChannel.data[i +ParticleChannels.ZOffset] += TMP_V3.z;
			}
		}

		@Override
		public PolarAcceleration copy () {
			return new PolarAcceleration(this);
		}
	}
	
	/*
	public static class ParallelPolar extends Angular {
		private class UpdateBlock extends Parallel.ForBlock{
			private Vector3 tmp = new Vector3();
			@Override
			public Void call () throws Exception {
				for(int 	i=startIndex*directionalVelocityChannel.strideSize, l = ParticleChannels.LifePercentOffset, s =0, a = 0,
					c = i +sliceSize*directionalVelocityChannel.strideSize; 
					i < c; 
					s += strengthChannel.strideSize, i +=directionalVelocityChannel.strideSize, 
						a += angularChannel.strideSize, l += lifeChannel.strideSize){

					float 	lifePercent = lifeChannel.data[l],
						strength = 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
						strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifePercent),
						phi = 	angularChannel.data[a + ParticleChannels.VelocityPhiStartOffset] + 
						angularChannel.data[a + ParticleChannels.VelocityPhiDiffOffset]* phiValue.getScale(lifePercent),
						theta = 	angularChannel.data[a + ParticleChannels.VelocityThetaStartOffset] + 
						angularChannel.data[a + ParticleChannels.VelocityThetaDiffOffset]* thetaValue.getScale(lifePercent);

					float cosTheta = MathUtils.cosDeg(theta), sinTheta = MathUtils.sinDeg(theta),
						cosPhi = MathUtils.cosDeg(phi), sinPhi = MathUtils.sinDeg(phi);
					tmp.set(cosTheta *sinPhi, cosPhi, sinTheta*sinPhi).nor().scl(strength);	
					directionalVelocityChannel.data[i +ParticleChannels.XOffset] += tmp.x;
					directionalVelocityChannel.data[i +ParticleChannels.YOffset] += tmp.y;
					directionalVelocityChannel.data[i +ParticleChannels.ZOffset] += tmp.z;
				}
				return null;
			}
		}

		FloatChannel directionalVelocityChannel;
		Array<UpdateBlock> updateBlocks;
		
		public ParallelPolar(){}
		
		public ParallelPolar (ParallelPolar rotation) {
			super(rotation);
		}

		@Override
		public void init () {
			super.init();
			if(updateBlocks == null){
				updateBlocks = new Array<VelocityModifier.ParallelPolar.UpdateBlock>();
				for(int i=0; i < Parallel.NUM_CORES; ++i){
					updateBlocks.add(new UpdateBlock());
				}
			}
		}
		
		@Override
		public void allocateChannels() {
			super.allocateChannels();
			directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Accelleration);
		}
		
		@Override
		public void update () {
			Parallel.blockingFor(0, controller.particles.size, updateBlocks);
		}

		@Override
		public ParallelPolar copy () {
			return new ParallelPolar(this);
		}
	}
	
	public static class Directional extends Strength {
		FloatChannel accelerationChannel, directionChannel;
		public ScaledNumericValue xValue, yValue, zValue;
		
		public Directional(){
			xValue = new ScaledNumericValue();
			yValue = new ScaledNumericValue();
			zValue = new ScaledNumericValue();
		}
		
		public Directional (Directional value) {
			super(value);
			xValue = new ScaledNumericValue();
			yValue = new ScaledNumericValue();
			zValue = new ScaledNumericValue();
			xValue.load(value.xValue);
			yValue.load(value.yValue);
			zValue.load(value.zValue);
		}
		
		@Override
		public void allocateChannels() {
			super.allocateChannels();
			ParticleChannels.Interpolation6.id = controller.particleChannels.newId();
			directionChannel = controller.particles.addChannel(ParticleChannels.Interpolation6);			
			accelerationChannel = controller.particles.addChannel(ParticleChannels.Accelleration);
		}
		
		@Override
		public void activateParticles (int startIndex, int count) {
			super.activateParticles(startIndex, count);
			float start, diff;
			for(int 	i=startIndex*directionChannel.strideSize, c = i +count*directionChannel.strideSize; 
				i < c; 
				i +=directionChannel.strideSize){
				//X
				start = xValue.newLowValue();
				diff = xValue.newHighValue();
				if(!xValue.isRelative())
					diff -= start;
				directionChannel.data[i + 0] = start;
				directionChannel.data[i + 1] = diff;
				//Y
				start = yValue.newLowValue();
				diff = yValue.newHighValue();
				if(!yValue.isRelative())
					diff -= start;
				directionChannel.data[i + 2] = start;
				directionChannel.data[i + 3] = diff;
				//Z
				start = zValue.newLowValue();
				diff = zValue.newHighValue();
				if(!zValue.isRelative())
					diff -= start;
				directionChannel.data[i + 4] = start;
				directionChannel.data[i + 5] = diff;
			}
		}

		@Override
		public void update () {
			for(int 	i=0, l = ParticleChannels.LifePercentOffset, s =0, a = 0,
				c = i +controller.particles.size*accelerationChannel.strideSize; 
				i < c; 
				s += strengthChannel.strideSize, i +=accelerationChannel.strideSize, 
				a += directionChannel.strideSize, l += lifeChannel.strideSize){
				
				float 	lifePercent = lifeChannel.data[l],
							strength = 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifePercent),
							x = 	directionChannel.data[a + 0] + 
										directionChannel.data[a + 1] * xValue.getScale(lifePercent),
							y = 	directionChannel.data[a + 2] + 
										directionChannel.data[a + 3] * yValue.getScale(lifePercent),
							z = 	directionChannel.data[a + 4] + 
										directionChannel.data[a + 5] * zValue.getScale(lifePercent);
				TMP_V3.set(x,y,z).nor().scl(strength);	
				accelerationChannel.data[i +ParticleChannels.XOffset] += TMP_V3.x;
				accelerationChannel.data[i +ParticleChannels.YOffset] += TMP_V3.y;
				accelerationChannel.data[i +ParticleChannels.ZOffset] += TMP_V3.z;
			}
		}

		@Override
		public Directional copy () {
			return new Directional(this);
		}
		
		@Override
		public void write (Json json) {
			super.write(json);
			json.writeValue("xValue", xValue);
			json.writeValue("yValue", yValue);
			json.writeValue("zValue", zValue);
		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			super.read(json, jsonData);
			xValue = json.readValue("xValue", ScaledNumericValue.class, jsonData);
			yValue = json.readValue("yValue", ScaledNumericValue.class, jsonData);
			zValue = json.readValue("zValue", ScaledNumericValue.class, jsonData);
		}
	}
	*/
	
	public static class TangentialAcceleration extends Angular {
		FloatChannel directionalVelocityChannel, positionChannel;
		
		public TangentialAcceleration(){}
		
		public TangentialAcceleration (TangentialAcceleration rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			directionalVelocityChannel = controller.particles.addChannel(ParticleChannels.Acceleration);
			positionChannel = controller.particles.addChannel(ParticleChannels.Position);
		}
		
		@Override
		public void update () {
			for(int 	i=0, l = ParticleChannels.LifePercentOffset, s =0, a = 0, positionOffset = 0,
				c = i +controller.particles.size*directionalVelocityChannel.strideSize; 
				i < c; 
				s += strengthChannel.strideSize, i +=directionalVelocityChannel.strideSize, 
				a += angularChannel.strideSize, l += lifeChannel.strideSize, positionOffset += positionChannel.strideSize ){
				
				float 	lifePercent = lifeChannel.data[l],
							strength = 	strengthChannel.data[s + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[s + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifePercent),
							phi = 	angularChannel.data[a + ParticleChannels.VelocityPhiStartOffset] + 
											angularChannel.data[a + ParticleChannels.VelocityPhiDiffOffset]* phiValue.getScale(lifePercent),
							theta = 	angularChannel.data[a + ParticleChannels.VelocityThetaStartOffset] + 
												angularChannel.data[a + ParticleChannels.VelocityThetaDiffOffset]* thetaValue.getScale(lifePercent);
				
				float cosTheta = MathUtils.cosDeg(theta), sinTheta = MathUtils.sinDeg(theta),
					cosPhi = MathUtils.cosDeg(phi), sinPhi = MathUtils.sinDeg(phi);
				TMP_V3.set(cosTheta *sinPhi, cosPhi, sinTheta*sinPhi)
								.crs(	positionChannel.data[positionOffset +ParticleChannels.XOffset], 
											positionChannel.data[positionOffset +ParticleChannels.YOffset], 
											positionChannel.data[positionOffset +ParticleChannels.ZOffset])
								.nor().scl(strength);	
				directionalVelocityChannel.data[i +ParticleChannels.XOffset] += TMP_V3.x;
				directionalVelocityChannel.data[i +ParticleChannels.YOffset] += TMP_V3.y;
				directionalVelocityChannel.data[i +ParticleChannels.ZOffset] += TMP_V3.z;
			}
		}

		@Override
		public TangentialAcceleration copy () {
			return new TangentialAcceleration(this);
		}
	}

	public static class BrownianAcceleration extends Strength {
		FloatChannel accelerationChannel;
		public BrownianAcceleration(){}
		
		public BrownianAcceleration (BrownianAcceleration rotation) {
			super(rotation);
		}

		@Override
		public void allocateChannels() {
			super.allocateChannels();
			accelerationChannel = controller.particles.addChannel(ParticleChannels.Acceleration);
		}
		
		@Override
		public void update () {
			int lifeOffset=ParticleChannels.LifePercentOffset, strengthOffset = 0, forceOffset = 0;
			for(int 	i=0,  c= controller.particles.size; i < c; ++i,  
				strengthOffset += strengthChannel.strideSize, 
				forceOffset +=accelerationChannel.strideSize, 
				lifeOffset += lifeChannel.strideSize){
			
				float 	strength = 	strengthChannel.data[strengthOffset + ParticleChannels.VelocityStrengthStartOffset] + 
													strengthChannel.data[strengthOffset + ParticleChannels.VelocityStrengthDiffOffset]* strengthValue.getScale(lifeChannel.data[lifeOffset]);
				TMP_V3.set(MathUtils.random(-1, 1f), MathUtils.random(-1, 1f), MathUtils.random(-1, 1f)).nor().scl(strength);
				accelerationChannel.data[forceOffset +ParticleChannels.XOffset] += TMP_V3.x;
				accelerationChannel.data[forceOffset +ParticleChannels.YOffset] += TMP_V3.y;
				accelerationChannel.data[forceOffset +ParticleChannels.ZOffset] += TMP_V3.z;
			}
		}

		@Override
		public BrownianAcceleration copy () {
			return new BrownianAcceleration(this);
		}
	}

	
	public boolean isGlobal = false;
	protected FloatChannel lifeChannel;
	
	public DynamicsModifier(){}
	
	public DynamicsModifier (DynamicsModifier modifier) {
		this.isGlobal = modifier.isGlobal;
	}

	@Override
	public void allocateChannels() {
		lifeChannel = controller.particles.addChannel(ParticleChannels.Life);
	}
	
	@Override
	public void write (Json json) {
		super.write(json);
		json.writeValue("isGlobal", isGlobal);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		isGlobal = json.readValue("isGlobal", boolean.class, jsonData);
	}
	
}

