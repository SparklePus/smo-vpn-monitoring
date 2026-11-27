package smo;

import java.util.Comparator;

public final class Buffer {
    public record Selection(int slot, Request request) { }

    private static final Comparator<Request> ARRIVAL_ORDER =
            Comparator.comparingDouble(Request::getCreatedAt).thenComparingLong(Request::getId);
    private final Request[] slots;
    private int size;

    public Buffer(int capacity) {
        slots = new Request[capacity];
    }

    public int insertFirstFree(Request request) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = request;
                size++;
                return i + 1;
            }
        }
        throw new IllegalStateException("Нет свободной ячейки");
    }

    public Selection peekOldest() { return select(ARRIVAL_ORDER); }
    public Selection evictOldest() { return remove(peekOldest()); }
    public Selection takeNewest() { return remove(select(ARRIVAL_ORDER.reversed())); }

    private Selection select(Comparator<Request> order) {
        int selected = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && (selected < 0 || order.compare(slots[i], slots[selected]) < 0)) {
                selected = i;
            }
        }
        return selected < 0 ? null : new Selection(selected + 1, slots[selected]);
    }

    private Selection remove(Selection selected) {
        if (selected != null) {
            slots[selected.slot() - 1] = null;
            size--;
        }
        return selected;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return size == slots.length; }
    public Request[] snapshot() { return slots.clone(); }
}
