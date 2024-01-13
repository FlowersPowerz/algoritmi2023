package connectx.BirbaBot;

import connectx.CXCell;

public class LabeledMove implements Comparable<LabeledMove> {
    private int value;
    private CXCell move;

    public LabeledMove(int label, CXCell cell) {
        this.value = label;
        this.move = cell;
    }

    public int getMove() {
        return this.move.j;
    }

    public CXCell getCell() {
        return move;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public int compareTo(LabeledMove other) {
        // Compare by value in ascending order (change the order if needed)
        return Integer.compare(other.getValue(), this.getValue());
    }
    // Using counting sort to sort the elements based on significant places in descending order
    static private void countingSortDescending(LabeledMove array[], int size, int place) {
        LabeledMove[] output = new LabeledMove[size];
        int[] count = new int[10];

        for (int i = 0; i < size; i++)
            count[9 - ((array[i].value / place) % 10)]++; // Invert the counting for descending order

        for (int i = 1; i < 10; i++)
            count[i] += count[i - 1];

        for (int i = size - 1; i >= 0; i--) {
            output[count[9 - ((array[i].value / place) % 10)] - 1] = array[i];
            count[9 - ((array[i].value / place) % 10)]--;
        }

        System.arraycopy(output, 0, array, 0, size);
    }

    // Function to get the largest element's value from an array
    static private int getMax(LabeledMove array[], int n) {
        int max = array[0].value;
        for (int i = 1; i < n; i++)
            if (array[i].value > max)
                max = array[i].value;
        return max;
    }

    // Main function to implement radix sort in descending order
    static public void radixSort(LabeledMove array[], int size) {
        // Get maximum element's value
        int max = getMax(array, size);

        // Apply counting sort to sort elements based on place value.
        for (int place = 1; max / place > 0; place *= 10)
            countingSortDescending(array, size, place);
    }
}