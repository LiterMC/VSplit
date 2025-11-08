package com.github.litermc.vsplit.accessor;

import com.github.litermc.vsplit.api.attachment.ISplitListener;

import java.util.Collection;

public interface ShipObjectServerAccessor {
	Collection<ISplitListener> vsplit$getSplitListeners();
}
