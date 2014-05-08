package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/** This class is used by {@link ParticleBatch} to sort the particles before render them.*/
/** @author Inferno */

public abstract class ParticleSorter {
	static final Vector3 TMP_V1 = new Vector3();
	
	public static class None extends ParticleSorter{
		int currentCapacity = 0;
		int[] indices;
		
		@Override
		public void ensureCapacity (int capacity) {
			if(currentCapacity < capacity){
				indices = new int[capacity];
				for(int i=0; i < capacity; ++i)
					indices[i] = i;
				currentCapacity = capacity;
			}
		}
		
		@Override
		public  <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData){
			return indices;
		}
	}
	
	/*
	public static class ParallelDistanceParticleSorter extends ParticleSorter{
		float cx, cy, cz;
		ParticleControllerRenderData data;
		int currentIndex;
		private class DistanceBlock extends Parallel.ForBlock{
			@Override
			public Void call () throws Exception {
				for(int k=startIndex*data.positionChannel.strideSize, i = currentIndex +startIndex, c = i + endIndex; i < c; ++i, k+=data.positionChannel.strideSize){
					distances[i] = cx*data.positionChannel.data[k+ParticleChannels.XOffset] + cy*data.positionChannel.data[k+ParticleChannels.YOffset] + cz*data.positionChannel.data[k+ParticleChannels.ZOffset];
					particleIndices[i] = i;
				}
				return null;
			}
		}
		
		private float[] distances;
		private int[] particleIndices, particleOffsets;
		private int currentSize = 0;
		private Pool<ParallelQuickSortTask> quickSortTaskPool;
		private Array<DistanceBlock> distanceBlocks;
		private Vector<Future> futures;
		
		public ParallelDistanceParticleSorter(){
			quickSortTaskPool = new Pool<ParallelQuickSortTask>(){
				@Override
				protected ParallelQuickSortTask newObject () {
					return new ParallelQuickSortTask(quickSortTaskPool);
				}
			};
			futures = new Vector();
			distanceBlocks = new Array<DistanceBlock>();
			for(int i=0; i < Parallel.NUM_CORES; ++i){
				distanceBlocks.add(new DistanceBlock());
			}
		}
		
		@Override
		public void ensureCapacity (int capacity) {
			if(currentSize < capacity){
				distances = new float[capacity];
				particleIndices = new int[capacity];
				particleOffsets = new int[capacity];
				currentSize = capacity;
			}
		}

		@Override
		public  <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData){
			float[] val = camera.view.val;
			cx = val[Matrix4.M20]; 
			cy = val[Matrix4.M21];
			cz = val[Matrix4.M22];
			currentIndex = 0;
			for(ParticleControllerRenderData data : renderData){
				this.data = data;
				Parallel.blockingFor(0, data.controller.particles.size, distanceBlocks);
				currentIndex += data.controller.particles.size;
			}
			
			ParallelQuickSortTask rootTask = quickSortTaskPool.obtain().set(futures, distances, particleIndices, 0, currentIndex -1); 
			futures.add(Parallel.executorService.submit(rootTask));
			try{
				while(!futures.isEmpty()){
					futures.remove(0).get();
				}
			}catch(InterruptedException ie){
				ie.printStackTrace();
			}catch(ExecutionException ie){
				ie.printStackTrace();
			}
			
			for(int i=0; i < currentIndex; ++i){
				particleOffsets[particleIndices[i]] = i;
			}
			return particleOffsets;
		}
	}
	*/
	
	public static class Distance extends ParticleSorter{
		private float[] distances;
		private int[] particleIndices, particleOffsets;
		private int currentSize = 0;
		
		@Override
		public void ensureCapacity (int capacity) {
			if(currentSize < capacity){
				distances = new float[capacity];
				particleIndices = new int[capacity];
				particleOffsets = new int[capacity];
				currentSize = capacity;
			}
		}

		@Override
		public  <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData){
			float[] val = camera.view.val;
			float cx = val[Matrix4.M20], cy = val[Matrix4.M21], cz = val[Matrix4.M22];
			int count = 0, i = 0;
			for(ParticleControllerRenderData data : renderData){
				for(int k=0, c = i+data.controller.particles.size; i <c; ++i, k+=data.positionChannel.strideSize){
					distances[i] = cx*data.positionChannel.data[k+ParticleChannels.XOffset] + cy*data.positionChannel.data[k+ParticleChannels.YOffset] + cz*data.positionChannel.data[k+ParticleChannels.ZOffset];
					particleIndices[i] = i;
				}
				count += data.controller.particles.size;
			}
			
			qsort(0, count-1);
			
			for(i=0; i < count; ++i){
				particleOffsets[particleIndices[i]] = i;
			}
			return particleOffsets;
		}

		public void qsort( int si, int ei){
			//base case
			if(si< ei){
				float tmp;
				int 	tmpIndex, particlesPivotIndex;
				//insertion
				if (ei-si <= 8) {
					for (int i=si; i <= ei; i++)
						for (int j=i; j > si && distances[j-1]>distances[j]; j--){
				           tmp = distances[j]; 
				           distances[j] = distances[j-1]; 
				           distances[j-1] = tmp;

				           //Swap indices
				           tmpIndex = particleIndices[j]; 
				           particleIndices[j] = particleIndices[j-1]; 
				           particleIndices[j-1] = tmpIndex;       
						}
					return;
				}
				
				//Quick
				float pivot = distances[si]; 
				int i = si+1;
				particlesPivotIndex = particleIndices[si];

				//partition array 
				for(int j = si+1; j<= ei; j++){
					if(pivot  > distances[j]){
						if(j>i){
							//Swap distances
							tmp = distances[j]; 
							distances[j] = distances[i]; 
							distances[i] = tmp;

							//Swap indices
							tmpIndex = particleIndices[j]; 
							particleIndices[j] = particleIndices[i]; 
							particleIndices[i] = tmpIndex;            
						}
						i++; 
					}
				}

				//put pivot in right position
				distances[si] = distances[i-1]; 
				distances[i-1] = pivot; 
				particleIndices[si] = particleIndices[i-1]; 
				particleIndices[i-1] = particlesPivotIndex;

				//call qsort on right and left sides of pivot
				qsort(si, i-2); 
				qsort(i, ei); 
			}
		}
	}

	protected Camera camera;

	public abstract <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData);
	
	public void setCamera(Camera camera){
		this.camera = camera;
	}
	
	public void ensureCapacity (int capacity) {}	
}
