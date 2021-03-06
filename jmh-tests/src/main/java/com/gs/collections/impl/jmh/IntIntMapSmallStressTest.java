/*
 * Copyright 2015 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gs.collections.impl.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.gs.collections.api.list.primitive.MutableIntList;
import com.gs.collections.api.map.primitive.MutableIntIntMap;
import com.gs.collections.api.set.primitive.MutableIntSet;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import com.gs.collections.impl.map.mutable.primitive.IntIntHashMap;
import com.gs.collections.impl.set.mutable.primitive.IntHashSet;
import net.openhft.koloboke.collect.map.IntIntMap;
import net.openhft.koloboke.collect.map.hash.HashIntIntMaps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class IntIntMapSmallStressTest
{
    private static final int LOOP_COUNT = 100;
    private static final int KEY_COUNT = 500;
    private static final int MAP_SIZE = 1_000;

    @Param({"true", "false"})
    public boolean fullyRandom;
    private IntIntMap intIntKoloboke;
    private MutableIntIntMap intIntGsc;
    private int[] gscIntKeysForMap;
    private int[] kolobokeIntKeysForMap;

    private int kolobokeIndex(int key)
    {
        int h = key * 0x9e3779b9;
        return this.mask(h ^ h >> 16);
    }

    private int gscIndex(int element)
    {
        int code = element;
        code ^= code >>> 15;
        code *= 0xACAB2A4D;
        code ^= code >>> 15;
        code *= 0x5CC7DF53;
        code ^= code >>> 12;
        return this.mask(code);
    }

    private int gscIndexTwo(int element)
    {
        int code = element;
        code ^= code >>> 14;
        code *= 0xBA1CCD33;
        code ^= code >>> 13;
        code *= 0x9B6296CB;
        code ^= code >>> 12;
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & ((1 << 11) - 1);
    }

    @Setup
    public void setUp()
    {
        this.intIntKoloboke = HashIntIntMaps.newMutableMap(MAP_SIZE);
        this.intIntGsc = new IntIntHashMap(MAP_SIZE);

        Random random = new Random(0x123456789ABCDL);

        int[] randomNumbersForMap = this.getRandomKeys(random).toArray();

        int number = 23;
        int lower = Integer.MIN_VALUE;
        int upper = Integer.MAX_VALUE;
        this.kolobokeIntKeysForMap = this.fullyRandom ? randomNumbersForMap : this.getKolobokeCollisions(number, lower, upper, random);
        this.gscIntKeysForMap = this.fullyRandom ? randomNumbersForMap : this.getGSCCollisions(number, lower, upper, random);

        for (int i = 0; i < KEY_COUNT; i++)
        {
            this.intIntKoloboke.put(this.kolobokeIntKeysForMap[i], 5);
            this.intIntGsc.put(this.gscIntKeysForMap[i], 5);
        }

        this.shuffle(this.gscIntKeysForMap, random);
        this.shuffle(this.kolobokeIntKeysForMap, random);
    }

    protected int[] getGSCCollisions(int number, int lower, int upper, Random random)
    {
        int[] gscCollisions = this.getGSCCollisions(number, lower, upper).toArray();
        this.shuffle(gscCollisions, random);
        return gscCollisions;
    }

    protected MutableIntList getGSCCollisions(int number, int lower, int upper)
    {
        MutableIntList gscCollidingNumbers = new IntArrayList();
        for (int i = lower; i < upper && gscCollidingNumbers.size() < KEY_COUNT; i++)
        {
            int index = this.gscIndex(i);
            if (index >= number && index <= number + 100)
            {
                gscCollidingNumbers.add(i);
            }
        }
        return gscCollidingNumbers;
    }

    protected int[] getKolobokeCollisions(int number, int lower, int upper, Random random)
    {
        int[] kolobokeCollisions = this.getKolobokeCollisions(number, lower, upper).toArray();
        this.shuffle(kolobokeCollisions, random);
        return kolobokeCollisions;
    }

    protected MutableIntList getKolobokeCollisions(int number, int lower, int upper)
    {
        MutableIntList kolobokeCollidingNumbers = new IntArrayList();
        for (int i = lower; i < upper && kolobokeCollidingNumbers.size() < KEY_COUNT; i++)
        {
            int index = this.kolobokeIndex(i);
            if (index >= number && index <= number + 100)
            {
                kolobokeCollidingNumbers.add(i);
            }
        }
        return kolobokeCollidingNumbers;
    }

    protected MutableIntSet getRandomKeys(Random random)
    {
        MutableIntSet set = new IntHashSet(KEY_COUNT);
        while (set.size() < KEY_COUNT)
        {
            set.add(random.nextInt());
        }

        return set;
    }

    @Benchmark
    public void kolobokeGet()
    {
        for (int j = 0; j < LOOP_COUNT; j++)
        {
            for (int i = 0; i < KEY_COUNT; i++)
            {
                if (this.intIntKoloboke.get(this.kolobokeIntKeysForMap[i]) == this.intIntKoloboke.defaultValue())
                {
                    throw new AssertionError(this.kolobokeIntKeysForMap[i] + " not in map");
                }
            }
            if (this.intIntKoloboke.size() != KEY_COUNT)
            {
                throw new AssertionError("size is " + this.intIntKoloboke.size());
            }
        }
    }

    @Benchmark
    public void gscGet()
    {
        for (int j = 0; j < LOOP_COUNT; j++)
        {
            for (int i = 0; i < KEY_COUNT; i++)
            {
                if (this.intIntGsc.get(this.gscIntKeysForMap[i]) == 0)
                {
                    throw new AssertionError(this.gscIntKeysForMap[i] + " not in map");
                }
            }
            if (this.intIntGsc.size() != KEY_COUNT)
            {
                throw new AssertionError("size is " + this.intIntGsc.size());
            }
        }
    }

    @Benchmark
    public void kolobokePut()
    {
        for (int j = 0; j < LOOP_COUNT; j++)
        {
            IntIntMap newMap = HashIntIntMaps.newMutableMap(MAP_SIZE);
            for (int i = 0; i < KEY_COUNT; i++)
            {
                newMap.put(this.kolobokeIntKeysForMap[i], 4);
            }
            if (newMap.size() != KEY_COUNT)
            {
                throw new AssertionError("size is " + newMap.size());
            }
        }
    }

    @Benchmark
    public void gscPut()
    {
        for (int j = 0; j < LOOP_COUNT; j++)
        {
            MutableIntIntMap newMap = new IntIntHashMap(MAP_SIZE);
            for (int i = 0; i < KEY_COUNT; i++)
            {
                newMap.put(this.gscIntKeysForMap[i], 4);
            }
            if (newMap.size() != KEY_COUNT)
            {
                throw new AssertionError("size is " + newMap.size());
            }
        }
    }

    public void shuffle(int[] intArray, Random rnd)
    {
        for (int i = intArray.length; i > 1; i--)
        {
            IntIntMapSmallStressTest.swap(intArray, i - 1, rnd.nextInt(i));
        }
    }

    private static void swap(int[] arr, int i, int j)
    {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
}
