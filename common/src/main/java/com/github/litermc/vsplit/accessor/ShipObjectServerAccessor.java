package com.github.litermc.vsplit.accessor;

import com.github.litermc.vsplit.api.attachment.ISplitListener;
import com.github.litermc.vsplit.api.clean.ICleanListener;

import java.util.Collection;

public interface ShipObjectServerAccessor {
	Collection<ICleanListener> vsplit$getCleanListeners();
	Collection<ISplitListener> vsplit$getSplitListeners();
}
