/*
 * Copyright 2014 Goldman Sachs.
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

package com.gs.collections.impl.set.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;

import com.gs.collections.api.ByteIterable;
import com.gs.collections.api.LazyByteIterable;
import com.gs.collections.api.bag.primitive.MutableByteBag;
import com.gs.collections.api.block.function.primitive.ByteToObjectFunction;
import com.gs.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import com.gs.collections.api.block.predicate.primitive.BytePredicate;
import com.gs.collections.api.block.procedure.primitive.ByteProcedure;
import com.gs.collections.api.iterator.ByteIterator;
import com.gs.collections.api.iterator.MutableByteIterator;
import com.gs.collections.api.list.primitive.MutableByteList;
import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.api.set.primitive.ByteSet;
import com.gs.collections.api.set.primitive.ImmutableByteSet;
import com.gs.collections.api.set.primitive.MutableByteSet;
import com.gs.collections.impl.bag.mutable.primitive.ByteHashBag;
import com.gs.collections.impl.block.procedure.checked.primitive.CheckedByteProcedure;
import com.gs.collections.impl.factory.primitive.ByteSets;
import com.gs.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import com.gs.collections.impl.list.mutable.primitive.ByteArrayList;
import com.gs.collections.impl.set.immutable.primitive.ImmutableByteSetSerializationProxy;
import com.gs.collections.impl.set.mutable.UnifiedSet;

public final class ByteHashSet implements MutableByteSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final byte MAX_BYTE_GROUP_1 = -65;
    private static final byte MAX_BYTE_GROUP_2 = -1;
    private static final byte MAX_BYTE_GROUP_3 = 63;
    private long bitGroup1; // -128 to -65
    private long bitGroup2; //-64 to -1
    private long bitGroup3; //0 to 63
    private long bitGroup4; // 64 to 127
    private short size;

    public ByteHashSet()
    {
    }

    /**
     * Use {@link ByteHashSet#ByteHashSet()} instead.
     *
     * @deprecated since 5.0.
     */
    @Deprecated
    public ByteHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
    }

    public ByteHashSet(ByteHashSet set)
    {
        this.size = set.size;
        this.bitGroup3 = set.bitGroup3;
        this.bitGroup4 = set.bitGroup4;
        this.bitGroup1 = set.bitGroup1;
        this.bitGroup2 = set.bitGroup2;
    }

    public ByteHashSet(byte... elements)
    {
        this();
        this.addAll(elements);
    }

    public static ByteHashSet newSet(ByteIterable source)
    {
        if (source instanceof ByteHashSet)
        {
            return new ByteHashSet((ByteHashSet) source);
        }

        return ByteHashSet.newSetWith(source.toArray());
    }

    public static ByteHashSet newSetWith(byte... source)
    {
        return new ByteHashSet(source);
    }

    public boolean add(byte element)
    {
        if (element <= MAX_BYTE_GROUP_1)
        {
            long initial = this.bitGroup1;

            this.bitGroup1 |= 1L << (byte) ((element + 1) * -1);

            if (this.bitGroup1 != initial)
            {
                this.size++;
                return true;
            }
        }
        else if (element <= MAX_BYTE_GROUP_2)
        {
            long initial = this.bitGroup2;

            this.bitGroup2 |= 1L << (byte) ((element + 1) * -1);

            if (this.bitGroup2 != initial)
            {
                this.size++;
                return true;
            }
        }
        else if (element <= MAX_BYTE_GROUP_3)
        {
            long initial = this.bitGroup3;

            this.bitGroup3 |= 1L << element;

            if (this.bitGroup3 != initial)
            {
                this.size++;
                return true;
            }
        }
        else
        {
            long initial = this.bitGroup4;

            this.bitGroup4 |= 1L << element;

            if (this.bitGroup4 != initial)
            {
                this.size++;
                return true;
            }
        }

        return false;
    }

    public boolean remove(byte value)
    {
        if (value <= MAX_BYTE_GROUP_1)
        {
            long initial = this.bitGroup1;
            this.bitGroup1 &= ~(1L << (byte) ((value + 1) * -1));
            if (this.bitGroup1 == initial)
            {
                return false;
            }
            this.size--;
            return true;
        }
        if (value <= MAX_BYTE_GROUP_2)
        {
            long initial = this.bitGroup2;
            this.bitGroup2 &= ~(1L << (byte) ((value + 1) * -1));

            if (this.bitGroup2 == initial)
            {
                return false;
            }
            this.size--;
            return true;
        }
        if (value <= MAX_BYTE_GROUP_3)
        {
            long initial = this.bitGroup3;
            this.bitGroup3 &= ~(1L << value);
            if (this.bitGroup3 == initial)
            {
                return false;
            }
            this.size--;
            return true;
        }

        long initial = this.bitGroup4;
        this.bitGroup4 &= ~(1L << value);
        if (this.bitGroup4 == initial)
        {
            return false;
        }
        this.size--;
        return true;
    }

    public boolean contains(byte value)
    {
        if (value <= MAX_BYTE_GROUP_1)
        {
            return ((this.bitGroup1 >>> (byte) ((value + 1) * -1)) & 1L) != 0;
        }
        if (value <= MAX_BYTE_GROUP_2)
        {
            return ((this.bitGroup2 >>> (byte) ((value + 1) * -1)) & 1L) != 0;
        }
        if (value <= MAX_BYTE_GROUP_3)
        {
            return ((this.bitGroup3 >>> value) & 1L) != 0;
        }

        return ((this.bitGroup4 >>> value) & 1L) != 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ByteSet))
        {
            return false;
        }

        ByteSet other = (ByteSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @Override
    public int hashCode()
    {
        return (int) this.sum();
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    public int size()
    {
        return this.size;
    }

    public boolean isEmpty()
    {
        return this.size() == 0;
    }

    public boolean notEmpty()
    {
        return this.size() != 0;
    }

    public String makeString()
    {
        return this.makeString(", ");
    }

    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            int count = 0;
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                if (count > 0)
                {
                    appendable.append(separator);
                }

                count++;
                appendable.append(String.valueOf(iterator.next()));
            }

            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean addAll(byte... source)
    {
        int oldSize = this.size();
        for (byte item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    public boolean addAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();

        if (source instanceof ByteHashSet)
        {
            ByteHashSet hashSet = (ByteHashSet) source;

            this.size = 0;
            this.bitGroup3 |= hashSet.bitGroup3;
            this.size += Long.bitCount(this.bitGroup3);

            this.bitGroup4 |= hashSet.bitGroup4;
            this.size += Long.bitCount(this.bitGroup4);

            this.bitGroup2 |= hashSet.bitGroup2;
            this.size += Long.bitCount(this.bitGroup2);

            this.bitGroup1 |= hashSet.bitGroup1;
            this.size += Long.bitCount(this.bitGroup1);
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    public boolean removeAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof ByteHashSet)
        {
            this.size = 0;
            ByteHashSet hashSet = (ByteHashSet) source;
            this.bitGroup3 &= ~hashSet.bitGroup3;
            this.size += Long.bitCount(this.bitGroup3);

            this.bitGroup4 &= ~hashSet.bitGroup4;
            this.size += Long.bitCount(this.bitGroup4);

            this.bitGroup2 &= ~hashSet.bitGroup2;
            this.size += Long.bitCount(this.bitGroup2);

            this.bitGroup1 &= ~hashSet.bitGroup1;
            this.size += Long.bitCount(this.bitGroup1);
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    public boolean removeAll(byte... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (byte item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    public boolean retainAll(ByteIterable source)
    {
        int oldSize = this.size();
        final ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();

        ByteHashSet retained = this.select(new BytePredicate()
        {
            public boolean accept(byte value)
            {
                return sourceSet.contains(value);
            }
        });
        if (retained.size() != oldSize)
        {
            this.bitGroup3 = retained.bitGroup3;
            this.bitGroup4 = retained.bitGroup4;
            this.bitGroup1 = retained.bitGroup1;
            this.bitGroup2 = retained.bitGroup2;
            this.size = retained.size;
            return true;
        }

        return false;
    }

    public boolean retainAll(byte... source)
    {
        return this.retainAll(ByteHashSet.newSetWith(source));
    }

    public void clear()
    {
        this.size = 0;
        this.bitGroup3 = 0L;
        this.bitGroup4 = 0L;
        this.bitGroup1 = 0L;
        this.bitGroup2 = 0L;
    }

    public ByteHashSet with(byte element)
    {
        this.add(element);
        return this;
    }

    public ByteHashSet without(byte element)
    {
        this.remove(element);
        return this;
    }

    public ByteHashSet withAll(ByteIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    public ByteHashSet withoutAll(ByteIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public MutableByteSet asUnmodifiable()
    {
        return new UnmodifiableByteSet(this);
    }

    public MutableByteSet asSynchronized()
    {
        return new SynchronizedByteSet(this);
    }

    public ImmutableByteSet toImmutable()
    {
        if (this.size() == 0)
        {
            return ByteSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return ByteSets.immutable.with(this.byteIterator().next());
        }
        ByteHashSet mutableSet = ByteHashSet.newSetWith(this.toArray());
        return new ImmutableByteHashSet(mutableSet.bitGroup3, mutableSet.bitGroup4,
                mutableSet.bitGroup1, mutableSet.bitGroup2, mutableSet.size);
    }

    public MutableByteIterator byteIterator()
    {
        return new MutableInternalByteIterator();
    }

    public byte[] toArray()
    {
        byte[] array = new byte[this.size()];
        int index = 0;

        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            byte nextByte = iterator.next();
            array[index] = nextByte;
            index++;
        }

        return array;
    }

    public boolean containsAll(byte... source)
    {
        for (byte item : source)
        {
            if (!this.contains(item))
            {
                return false;
            }
        }
        return true;
    }

    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    public void forEach(ByteProcedure procedure)
    {
        long bitGroup1 = this.bitGroup1;
        while (bitGroup1 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup1);
            procedure.value((byte) ((value + 65) * -1));
            bitGroup1 &= ~(1L << (byte) (value + 64));
        }

        long bitGroup2 = this.bitGroup2;
        while (bitGroup2 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup2);
            procedure.value((byte) ((value + 1) * -1));
            bitGroup2 &= ~(1L << value);
        }

        long bitGroup3 = this.bitGroup3;
        while (bitGroup3 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup3);
            procedure.value(value);
            bitGroup3 &= ~(1L << value);
        }

        long bitGroup4 = this.bitGroup4;
        while (bitGroup4 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup4);
            procedure.value((byte) (value + 64));
            bitGroup4 &= ~(1L << (byte) (value + 64));
        }
    }

    public ByteHashSet select(final BytePredicate predicate)
    {
        final ByteHashSet result = new ByteHashSet();

        this.forEach(new ByteProcedure()
        {
            public void value(byte value)
            {
                if (predicate.accept(value))
                {
                    result.add(value);
                }
            }
        });

        return result;
    }

    public MutableByteSet reject(final BytePredicate predicate)
    {
        final MutableByteSet result = new ByteHashSet();

        this.forEach(new ByteProcedure()
        {
            public void value(byte value)
            {
                if (!predicate.accept(value))
                {
                    result.add(value);
                }
            }
        });

        return result;
    }

    public <V> MutableSet<V> collect(final ByteToObjectFunction<? extends V> function)
    {
        final MutableSet<V> target = UnifiedSet.newSet(this.size());

        this.forEach(new ByteProcedure()
        {
            public void value(byte each)
            {
                target.add(function.valueOf(each));
            }
        });

        return target;
    }

    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            byte nextByte = iterator.next();

            if (predicate.accept(nextByte))
            {
                return nextByte;
            }
        }

        return ifNone;
    }

    public int count(BytePredicate predicate)
    {
        int count = 0;
        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                count++;
            }
        }

        return count;
    }

    public boolean anySatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean allSatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            if (!predicate.accept(iterator.next()))
            {
                return false;
            }
        }

        return true;
    }

    public boolean noneSatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return false;
            }
        }

        return true;
    }

    public MutableByteList toList()
    {
        return ByteArrayList.newList(this);
    }

    public MutableByteSet toSet()
    {
        return ByteHashSet.newSet(this);
    }

    public MutableByteBag toBag()
    {
        return ByteHashBag.newBag(this);
    }

    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    public long sum()
    {
        long result = 0L;

        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            result += iterator.next();
        }

        return result;
    }

    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        byte max = 0;

        if (this.bitGroup4 != 0L)
        {
            //the highest has to be from this
            max = (byte) (127 - Long.numberOfLeadingZeros(this.bitGroup4));
        }
        else if (this.bitGroup3 != 0L)
        {
            max = (byte) (63 - Long.numberOfLeadingZeros(this.bitGroup3));
        }
        else if (this.bitGroup2 != 0L)
        {
            max = (byte) ((Long.numberOfTrailingZeros(this.bitGroup2) + 1) * -1);
        }
        else if (this.bitGroup1 != 0L)
        {
            max = (byte) ((Long.numberOfTrailingZeros(this.bitGroup1) + 65) * -1);
        }

        return max;
    }

    public byte maxIfEmpty(byte defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.max();
    }

    public byte min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        byte min = 0;

        if (this.bitGroup1 != 0L)
        {
            //the minimum has to be from this
            min = (byte) (128 - Long.numberOfLeadingZeros(this.bitGroup1));
            min *= -1;
        }
        else if (this.bitGroup2 != 0L)
        {
            min = (byte) ((64 - Long.numberOfLeadingZeros(this.bitGroup2)) * -1);
        }
        else if (this.bitGroup3 != 0L)
        {
            min = (byte) Long.numberOfTrailingZeros(this.bitGroup3);
        }
        else if (this.bitGroup4 != 0L)
        {
            min = (byte) (Long.numberOfTrailingZeros(this.bitGroup4) + 64);
        }

        return min;
    }

    public byte minIfEmpty(byte defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.size();
    }

    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        byte[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            byte first = sortedArray[middleIndex];
            byte second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    public byte[] toSortedArray()
    {
        byte[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    public MutableByteList toSortedList()
    {
        return ByteArrayList.newList(this).sortThis();
    }

    public ByteSet freeze()
    {
        if (this.size() == 0)
        {
            return ByteSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return ByteSets.immutable.with(this.byteIterator().next());
        }

        return new ImmutableByteHashSet(this.bitGroup3, this.bitGroup4,
                this.bitGroup1, this.bitGroup2, this.size);
    }

    public void writeExternal(final ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        this.forEach(new CheckedByteProcedure()
        {
            public void safeValue(byte each) throws IOException
            {
                out.writeByte(each);
            }
        });
    }

    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readByte());
        }
    }

    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;

        ByteIterator iterator = this.byteIterator();

        while (iterator.hasNext())
        {
            result = function.valueOf(result, iterator.next());
        }

        return result;
    }

    private static final class ImmutableByteHashSet implements ImmutableByteSet, Serializable
    {
        private static final long serialVersionUID = 1L;
        private final long bitGroup1; // -128 to -65
        private final long bitGroup2; //-64 to -1
        private final long bitGroup3; //0 to 63
        private final long bitGroup4; // 64 to 127
        private final short size;

        private ImmutableByteHashSet(long bitGroup3, long bitGroup4,
                long bitGroup1, long bitGroup2, short size)
        {
            this.bitGroup3 = bitGroup3;
            this.bitGroup4 = bitGroup4;
            this.bitGroup1 = bitGroup1;
            this.bitGroup2 = bitGroup2;
            this.size = size;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (!(obj instanceof ByteSet))
            {
                return false;
            }

            ByteSet other = (ByteSet) obj;
            return this.size() == other.size() && this.containsAll(other.toArray());
        }

        @Override
        public int hashCode()
        {
            return (int) this.sum();
        }

        @Override
        public String toString()
        {
            return this.makeString("[", ", ", "]");
        }

        public ImmutableByteSet newWith(byte element)
        {
            return ByteHashSet.newSet(this).with(element).toImmutable();
        }

        public ImmutableByteSet newWithout(byte element)
        {
            return ByteHashSet.newSet(this).without(element).toImmutable();
        }

        public ImmutableByteSet newWithAll(ByteIterable elements)
        {
            return ByteHashSet.newSet(this).withAll(elements).toImmutable();
        }

        public ImmutableByteSet newWithoutAll(ByteIterable elements)
        {
            return ByteHashSet.newSet(this).withoutAll(elements).toImmutable();
        }

        public int size()
        {
            return this.size;
        }

        public boolean isEmpty()
        {
            return this.size() == 0;
        }

        public boolean notEmpty()
        {
            return this.size() != 0;
        }

        public String makeString()
        {
            return this.makeString(", ");
        }

        public String makeString(String separator)
        {
            return this.makeString("", separator, "");
        }

        public String makeString(String start, String separator, String end)
        {
            Appendable stringBuilder = new StringBuilder();
            this.appendString(stringBuilder, start, separator, end);
            return stringBuilder.toString();
        }

        public void appendString(Appendable appendable)
        {
            this.appendString(appendable, ", ");
        }

        public void appendString(Appendable appendable, String separator)
        {
            this.appendString(appendable, "", separator, "");
        }

        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);
                int count = 0;
                ByteIterator iterator = this.byteIterator();

                while (iterator.hasNext())
                {
                    byte nextByte = iterator.next();

                    if (count > 0)
                    {
                        appendable.append(separator);
                    }

                    count++;
                    appendable.append(String.valueOf(nextByte));
                }

                appendable.append(end);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public ByteIterator byteIterator()
        {
            return new InternalByteIterator();
        }

        public byte[] toArray()
        {
            byte[] array = new byte[this.size()];
            int index = 0;

            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                byte nextByte = iterator.next();
                array[index] = nextByte;
                index++;
            }

            return array;
        }

        public boolean contains(byte value)
        {
            if (value <= MAX_BYTE_GROUP_1)
            {
                return ((this.bitGroup1 >>> (byte) ((value + 1) * -1)) & 1L) != 0;
            }
            if (value <= MAX_BYTE_GROUP_2)
            {
                return ((this.bitGroup2 >>> (byte) ((value + 1) * -1)) & 1L) != 0;
            }
            if (value <= MAX_BYTE_GROUP_3)
            {
                return ((this.bitGroup3 >>> value) & 1L) != 0;
            }

            return ((this.bitGroup4 >>> value) & 1L) != 0;
        }

        public boolean containsAll(byte... source)
        {
            for (byte item : source)
            {
                if (!this.contains(item))
                {
                    return false;
                }
            }
            return true;
        }

        public boolean containsAll(ByteIterable source)
        {
            for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
            {
                if (!this.contains(iterator.next()))
                {
                    return false;
                }
            }
            return true;
        }

        public void forEach(ByteProcedure procedure)
        {
            long bitGroup1 = this.bitGroup1;
            while (bitGroup1 != 0L)
            {
                byte value = (byte) Long.numberOfTrailingZeros(bitGroup1);
                procedure.value((byte) ((value + 65) * -1));
                bitGroup1 &= ~(1L << (byte) (value + 64));
            }

            long bitGroup2 = this.bitGroup2;
            while (bitGroup2 != 0L)
            {
                byte value = (byte) Long.numberOfTrailingZeros(bitGroup2);
                procedure.value((byte) ((value + 1) * -1));
                bitGroup2 &= ~(1L << value);
            }

            long bitGroup3 = this.bitGroup3;
            while (bitGroup3 != 0L)
            {
                byte value = (byte) Long.numberOfTrailingZeros(bitGroup3);
                procedure.value(value);
                bitGroup3 &= ~(1L << value);
            }

            long bitGroup4 = this.bitGroup4;
            while (bitGroup4 != 0L)
            {
                byte value = (byte) Long.numberOfTrailingZeros(bitGroup4);
                procedure.value((byte) (value + 64));
                bitGroup4 &= ~(1L << (byte) (value + 64));
            }
        }

        public ImmutableByteSet select(final BytePredicate predicate)
        {
            final MutableByteSet result = new ByteHashSet();

            this.forEach(new ByteProcedure()
            {
                public void value(byte value)
                {
                    if (predicate.accept(value))
                    {
                        result.add(value);
                    }
                }
            });

            return result.toImmutable();
        }

        public ImmutableByteSet reject(final BytePredicate predicate)
        {
            final MutableByteSet result = new ByteHashSet();

            this.forEach(new ByteProcedure()
            {
                public void value(byte value)
                {
                    if (!predicate.accept(value))
                    {
                        result.add(value);
                    }
                }
            });

            return result.toImmutable();
        }

        public <V> ImmutableSet<V> collect(final ByteToObjectFunction<? extends V> function)
        {
            final MutableSet<V> target = UnifiedSet.newSet(this.size());

            this.forEach(new ByteProcedure()
            {
                public void value(byte each)
                {
                    target.add(function.valueOf(each));
                }
            });

            return target.toImmutable();
        }

        public byte detectIfNone(BytePredicate predicate, byte ifNone)
        {
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                byte nextByte = iterator.next();

                if (predicate.accept(nextByte))
                {
                    return nextByte;
                }
            }

            return ifNone;
        }

        public int count(BytePredicate predicate)
        {
            int count = 0;
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                if (predicate.accept(iterator.next()))
                {
                    count++;
                }
            }

            return count;
        }

        public boolean anySatisfy(BytePredicate predicate)
        {
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                if (predicate.accept(iterator.next()))
                {
                    return true;
                }
            }

            return false;
        }

        public boolean allSatisfy(BytePredicate predicate)
        {
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                if (!predicate.accept(iterator.next()))
                {
                    return false;
                }
            }

            return true;
        }

        public boolean noneSatisfy(BytePredicate predicate)
        {
            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                if (predicate.accept(iterator.next()))
                {
                    return false;
                }
            }

            return true;
        }

        public MutableByteList toList()
        {
            return ByteArrayList.newList(this);
        }

        public MutableByteSet toSet()
        {
            return ByteHashSet.newSet(this);
        }

        public MutableByteBag toBag()
        {
            return ByteHashBag.newBag(this);
        }

        public LazyByteIterable asLazy()
        {
            return new LazyByteIterableAdapter(this);
        }

        public long sum()
        {
            long result = 0L;

            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                result += iterator.next();
            }

            return result;
        }

        public byte max()
        {
            byte max = 0;

            if (this.bitGroup4 != 0L)
            {
                //the highest has to be from this
                max = (byte) (127 - Long.numberOfLeadingZeros(this.bitGroup4));
            }
            else if (this.bitGroup3 != 0L)
            {
                max = (byte) (63 - Long.numberOfLeadingZeros(this.bitGroup3));
            }
            else if (this.bitGroup2 != 0L)
            {
                max = (byte) ((Long.numberOfTrailingZeros(this.bitGroup2) + 1) * -1);
            }
            else if (this.bitGroup1 != 0L)
            {
                max = (byte) ((Long.numberOfTrailingZeros(this.bitGroup1) + 65) * -1);
            }

            return max;
        }

        public byte maxIfEmpty(byte defaultValue)
        {
            return this.max();
        }

        public byte min()
        {
            byte min = 0;

            if (this.bitGroup1 != 0L)
            {
                //the minimum has to be from this
                min = (byte) (128 - Long.numberOfLeadingZeros(this.bitGroup1));
                min *= -1;
            }
            else if (this.bitGroup2 != 0L)
            {
                min = (byte) ((64 - Long.numberOfLeadingZeros(this.bitGroup2)) * -1);
            }
            else if (this.bitGroup3 != 0L)
            {
                min = (byte) Long.numberOfTrailingZeros(this.bitGroup3);
            }
            else if (this.bitGroup4 != 0L)
            {
                min = (byte) (Long.numberOfTrailingZeros(this.bitGroup4) + 64);
            }

            return min;
        }

        public byte minIfEmpty(byte defaultValue)
        {
            return this.min();
        }

        public double average()
        {
            return (double) this.sum() / (double) this.size();
        }

        public double median()
        {
            byte[] sortedArray = this.toSortedArray();
            int middleIndex = sortedArray.length >> 1;
            if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
            {
                byte first = sortedArray[middleIndex];
                byte second = sortedArray[middleIndex - 1];
                return ((double) first + (double) second) / 2.0;
            }
            return (double) sortedArray[middleIndex];
        }

        public byte[] toSortedArray()
        {
            byte[] array = this.toArray();
            Arrays.sort(array);
            return array;
        }

        public MutableByteList toSortedList()
        {
            return ByteArrayList.newList(this).sortThis();
        }

        public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;

            ByteIterator iterator = this.byteIterator();

            while (iterator.hasNext())
            {
                result = function.valueOf(result, iterator.next());
            }

            return result;
        }

        public ByteSet freeze()
        {
            return this;
        }

        public ImmutableByteSet toImmutable()
        {
            return this;
        }

        private Object writeReplace()
        {
            return new ImmutableByteSetSerializationProxy(this);
        }

        private class InternalByteIterator implements ByteIterator
        {
            private int count;
            private byte minusOneTwentyEightToPlusOneTwentySeven = -128;

            public boolean hasNext()
            {
                return this.count < ImmutableByteHashSet.this.size();
            }

            public byte next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.minusOneTwentyEightToPlusOneTwentySeven <= 127)
                {
                    if (ImmutableByteHashSet.this.contains(this.minusOneTwentyEightToPlusOneTwentySeven))
                    {
                        byte result = this.minusOneTwentyEightToPlusOneTwentySeven;
                        this.minusOneTwentyEightToPlusOneTwentySeven++;
                        return result;
                    }
                    this.minusOneTwentyEightToPlusOneTwentySeven++;
                }

                throw new NoSuchElementException("no more element, unexpected situation");
            }
        }
    }

    private class MutableInternalByteIterator implements MutableByteIterator
    {
        private int count;
        private byte minusOneTwentyEightToPlusOneTwentySeven = -128;

        public boolean hasNext()
        {
            return this.count < ByteHashSet.this.size();
        }

        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }

            this.count++;

            while (this.minusOneTwentyEightToPlusOneTwentySeven <= 127)
            {
                if (ByteHashSet.this.contains(this.minusOneTwentyEightToPlusOneTwentySeven))
                {
                    byte result = this.minusOneTwentyEightToPlusOneTwentySeven;
                    this.minusOneTwentyEightToPlusOneTwentySeven++;
                    return result;
                }
                this.minusOneTwentyEightToPlusOneTwentySeven++;
            }

            throw new NoSuchElementException("no more element, unexpected situation");
        }

        public void remove()
        {
            if (this.count == 0 || !ByteHashSet.this.remove((byte) (this.minusOneTwentyEightToPlusOneTwentySeven - 1)))
            {
                throw new IllegalStateException();
            }
            this.count--;
        }
    }
}
