package connectx.BirbaBot;

import connectx.CXCell;

public class LabeledMove implements Comparable<LabeledMove>{
    private int value;
    private CXCell move;

    public LabeledMove(int label, CXCell cell) {
        this.value = label;
        this.move = cell;
    }

    public int getMove() {
        return this.move.j;
    }

    public int getValue() {
        return this.value;
    }

	@Override
    public int compareTo(LabeledMove other) {
        // Compare by value in ascending order (change the order if needed)
        return Integer.compare(other.getValue(), this.getValue());
    }
    // Helper function to get the digit at a specific place for a value
    private static int getDigit(int value, int place) {
        return (Math.abs(value) / (int)Math.pow(10, place)) % 10;
    }

    // Counting sort for descending order
    private static void countingSortDescending(LabeledMove array[], int size, int place) {
        LabeledMove[] output = new LabeledMove[size];
        int[] count = new int[10];

        for (int i = 0; i < size; i++) {
            int digit = getDigit(array[i].getValue(), place);
            count[digit]++;
        }

        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }

        for (int i = size - 1; i >= 0; i--) {
            int digit = getDigit(array[i].getValue(), place);
            output[count[digit] - 1] = array[i];
            count[digit]--;
        }

        System.arraycopy(output, 0, array, 0, size);
    }

    // Main function to implement radix sort in descending order for both positive and negative values
    public static void radixSort(LabeledMove array[], int size) {
        int maxDigits = 0;
        for (int i = 0; i < size; i++) {
            int numDigits = (int)(Math.log10(Math.abs(array[i].getValue())) + 1);
            if (numDigits > maxDigits) {
                maxDigits = numDigits;
            }
        }

        for (int place = 0; place < maxDigits; place++) {
            countingSortDescending(array, size, place);
        }
    }
}
