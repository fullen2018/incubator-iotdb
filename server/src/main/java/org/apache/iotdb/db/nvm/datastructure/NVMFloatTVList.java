package org.apache.iotdb.db.nvm.datastructure;

import static org.apache.iotdb.db.nvm.rescon.NVMPrimitiveArrayPool.ARRAY_SIZE;

import java.util.ArrayList;
import java.util.List;
import org.apache.iotdb.db.nvm.rescon.NVMPrimitiveArrayPool;
import org.apache.iotdb.db.nvm.space.NVMSpaceManager.NVMSpace;
import org.apache.iotdb.db.rescon.PrimitiveArrayPool;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;

public class NVMFloatTVList extends NVMTVList {

  private List<NVMSpace> values;

  // TODO
  private float[][] sortedValues;

  private float pivotValue;

  NVMFloatTVList() {
    super();
    values = new ArrayList<>();
  }

  @Override
  public void putFloat(long timestamp, float value) {
    checkExpansion();
    int arrayIndex = size / ARRAY_SIZE;
    int elementIndex = size % ARRAY_SIZE;
    minTime = minTime <= timestamp ? minTime : timestamp;
    timestamps.get(arrayIndex).set(elementIndex, timestamp);
    values.get(arrayIndex).set(elementIndex, value);
    size++;
    if (sorted && size > 1 && timestamp < getTime(size - 2)) {
      sorted = false;
    }
  }

  @Override
  public float getFloat(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    int arrayIndex = index / ARRAY_SIZE;
    int elementIndex = index % ARRAY_SIZE;
    return (float) values.get(arrayIndex).get(elementIndex);
  }

  protected void set(int index, long timestamp, float value) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    int arrayIndex = index / ARRAY_SIZE;
    int elementIndex = index % ARRAY_SIZE;
    timestamps.get(arrayIndex).set(elementIndex, timestamp);
    values.get(arrayIndex).set(elementIndex, value);
  }

  @Override
  public NVMFloatTVList clone() {
    NVMFloatTVList cloneList = new NVMFloatTVList();
    cloneAs(cloneList);
    for (NVMSpace valueSpace : values) {
      cloneList.values.add(cloneValue(valueSpace));
    }
    return cloneList;
  }

  private NVMSpace cloneValue(NVMSpace valueSpace) {
    return valueSpace.clone();
  }

  @Override
  public void sort() {
    if (sortedTimestamps == null || sortedTimestamps.length < size) {
      sortedTimestamps = (long[][]) PrimitiveArrayPool
          .getInstance().getDataListsByType(TSDataType.INT64, size);
    }
    if (sortedValues == null || sortedValues.length < size) {
      sortedValues = (float[][]) PrimitiveArrayPool
          .getInstance().getDataListsByType(TSDataType.FLOAT, size);
    }
    sort(0, size);
    clearSortedValue();
    clearSortedTime();
    sorted = true;
  }

  @Override
  protected void clearValue() {
    if (values != null) {
      for (NVMSpace valueSpace : values) {
        PrimitiveArrayPool.getInstance().release(valueSpace);
      }
      values.clear();
    }
  }

  @Override
  protected void clearSortedValue() {
    if (sortedValues != null) {
      for (float[] dataArray : sortedValues) {
        PrimitiveArrayPool.getInstance().release(dataArray);
      }
      sortedValues = null;
    }
  }

  @Override
  protected void setFromSorted(int src, int dest) {
    set(dest, sortedTimestamps[src/ARRAY_SIZE][src%ARRAY_SIZE], sortedValues[src/ARRAY_SIZE][src%ARRAY_SIZE]);
  }

  @Override
  protected void set(int src, int dest) {
    long srcT = getTime(src);
    float srcV = getFloat(src);
    set(dest, srcT, srcV);
  }

  @Override
  protected void setToSorted(int src, int dest) {
    sortedTimestamps[dest/ARRAY_SIZE][dest% ARRAY_SIZE] = getTime(src);
    sortedValues[dest/ARRAY_SIZE][dest%ARRAY_SIZE] = getFloat(src);
  }

  @Override
  protected void reverseRange(int lo, int hi) {
    hi--;
    while (lo < hi) {
      long loT = getTime(lo);
      float loV = getFloat(lo);
      long hiT = getTime(hi);
      float hiV = getFloat(hi);
      set(lo++, hiT, hiV);
      set(hi--, loT, loV);
    }
  }

  @Override
  protected void expandValues() {
    values.add(NVMPrimitiveArrayPool
        .getInstance().getPrimitiveDataListByType(TSDataType.FLOAT));
  }

  @Override
  protected void saveAsPivot(int pos) {
    pivotTime = getTime(pos);
    pivotValue = getFloat(pos);
  }

  @Override
  protected void setPivotTo(int pos) {
    set(pos, pivotTime, pivotValue);
  }

  @Override
  protected void releaseLastValueArray() {
    PrimitiveArrayPool.getInstance().release(values.remove(values.size() - 1));
  }

  @Override
  public void putFloats(long[] time, float[] value) {
    checkExpansion();
    int idx = 0;
    int length = time.length;

    for (int i = 0; i < length; i++) {
      putFloat(time[i], value[i]);
    }

//    updateMinTimeAndSorted(time);
//
//    while (idx < length) {
//      int inputRemaining = length - idx;
//      int arrayIdx = size / ARRAY_SIZE;
//      int elementIdx = size % ARRAY_SIZE;
//      int internalRemaining  = ARRAY_SIZE - elementIdx;
//      if (internalRemaining >= inputRemaining) {
//        // the remaining inputs can fit the last array, copy all remaining inputs into last array
//        System.arraycopy(time, idx, timestamps.get(arrayIdx), elementIdx, inputRemaining);
//        System.arraycopy(value, idx, values.get(arrayIdx), elementIdx, inputRemaining);
//        size += inputRemaining;
//        break;
//      } else {
//        // the remaining inputs cannot fit the last array, fill the last array and create a new
//        // one and enter the next loop
//        System.arraycopy(time, idx, timestamps.get(arrayIdx), elementIdx, internalRemaining);
//        System.arraycopy(value, idx, values.get(arrayIdx), elementIdx, internalRemaining);
//        idx += internalRemaining;
//        size += internalRemaining;
//        checkExpansion();
//      }
//    }
  }
}