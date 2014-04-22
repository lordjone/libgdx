package com.badlogic.gdx.graphics.g3d.particles;

import java.util.Arrays;
import java.util.Comparator;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Sort;
import com.badlogic.gdx.utils.reflect.ArrayReflection;

/** This class is used by {@link ParticleBatch} to sort the particles before render them.*/
/** @author Inferno */
public abstract class ParticleSorter<T> {
	public static abstract class DistanceParticleSorter<T> extends ParticleSorter<T>{
		
		//private long[] indexDistanceArray;
		protected T[] out;
		protected float[] distances;
		protected int[] indices;
		private int currentSize = 0;
		private Class<T> type;
		
		public DistanceParticleSorter (Class<T> type) {
			this.type = type;
		}
		
		@Override
		public void ensureCapacity (int capacity) {
			if(currentSize < capacity){
				//indexDistanceArray = new long[capacity];
				indices = new int[capacity];
				distances = new float[capacity];
				out = (T[])ArrayReflection.newInstance(type, capacity);
				currentSize = capacity;
			}
		}
		
		@Override
		public T[] sort(T[] particles, int count){
			calculateDistances(particles, count);
			qsort(0, count-1);
			for(int k=0; k < count; ++k){
				out[k] = particles[indices[k]];
			}
			return out;
		}
		
		protected abstract void calculateDistances (T[] particles, int count);
		
		public void qsort( int si, int ei){
			//base case
			if(ei<=si || si>=ei){}

			else{ 
				float pivot = distances[si]; 
				int i = si+1; float tmp; 
				int tmpIndex, pivotIndex = indices[si];

				//partition array 
				for(int j = si+1; j<= ei; j++){
					if(pivot  > distances[j]){
						//Swap distances
						tmp = distances[j]; 
						distances[j] = distances[i]; 
						distances[i] = tmp;
						//Swap indices
						tmpIndex = indices[j]; 
						indices[j] = indices[i]; 
						indices[i] = tmpIndex;		                
						
						i++; 
					}
				}

				//put pivot in right position
				distances[si] = distances[i-1]; 
				distances[i-1] = pivot; 
				indices[si] = indices[i-1]; 
				indices[i-1] = pivotIndex; 

				//call qsort on right and left sides of pivot
				qsort(si, i-2); 
				qsort(i, ei); 
			}
		}
	}
	

	public static class BillboardDistanceParticleSorter extends DistanceParticleSorter<BillboardParticle>{
		public BillboardDistanceParticleSorter(){
			super(BillboardParticle.class);
		}
		
		@Override
		protected void calculateDistances (BillboardParticle[] particles, int count) {
			float[] val = camera.view.val;
			float cx = val[Matrix4.M20];
			float cy = val[Matrix4.M21];
			float cz = val[Matrix4.M22];
			for(int i=0; i <count; ++i){
				BillboardParticle particle = particles[i];
				distances[i] = cx*particle.x + cy*particle.y +cz*particle.z; //dot
				indices[i] = i;
			}
		}
	}
	
	public static class PointSpriteDistanceParticleSorter extends DistanceParticleSorter<PointSpriteParticle>{
		
		public PointSpriteDistanceParticleSorter () {
			super(PointSpriteParticle.class);
		}

		@Override
		protected void calculateDistances (PointSpriteParticle[] particles, int count) {
			float[] val = camera.view.val;
			float cx = val[Matrix4.M20];
			float cy = val[Matrix4.M21];
			float cz = val[Matrix4.M22];
			for(int i=0; i <count; ++i){
				PointSpriteParticle particle = particles[i];
				distances[i] = cx*particle.x + cy*particle.y +cz*particle.z; //dot
			}
		}
	}
	
	protected Camera camera;

	public void ensureCapacity (int capacity) {}
		
	public abstract T[] sort(T[] particles, int count);
	
	public void setCamera(Camera camera){
		this.camera = camera;
	}
	
	/*
	public static void qsort(Particle[] a, int si, int ei){
	    //base case
	    if(ei<=si || si>=ei){}

	    else{ 
	        Particle pivot = a[si]; 
	        int i = si+1; Particle tmp; 

	        //partition array 
	        for(int j = si+1; j<= ei; j++){
	            if(pivot.cameraDistance > a[j].cameraDistance){
	                tmp = a[j]; 
	                a[j] = a[i]; 
	                a[i] = tmp; 

	                i++; 
	            }
	        }

	        //put pivot in right position
	        a[si] = a[i-1]; 
	        a[i-1] = pivot; 

	        //call qsort on right and left sides of pivot
	        qsort(a, si, i-2); 
	        qsort(a, i, ei); 
	    }
	}
	*/
}
