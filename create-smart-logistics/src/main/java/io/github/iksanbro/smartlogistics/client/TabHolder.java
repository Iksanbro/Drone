package io.github.iksanbro.smartlogistics.client;

import org.jetbrains.annotations.Nullable;

/** A screen with an address tab (the stock keeper's menu of a smart stock ticker). */
public interface TabHolder {
    @Nullable
    AddressPanel smartlogistics$panel();
}
