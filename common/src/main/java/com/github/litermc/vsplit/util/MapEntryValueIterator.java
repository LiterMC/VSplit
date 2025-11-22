package com.github.litermc.vsplit.util;

import java.util.Iterator;
import java.util.Map;

public class MapEntryValueIterator<K, V> implements Iterator<V> {
	private final Iterator<Map.Entry<K, V>> original;

	public MapEntryValueIterator(final Iterator<Map.Entry<K, V>> original) {
		this.original = original;
	}

	@Override
	public boolean hasNext() {
		return this.original.hasNext();
	}

	@Override
	public V next() {
		return this.original.next().getValue();
	}
}
