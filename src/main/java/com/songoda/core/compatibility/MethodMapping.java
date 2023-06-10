package com.songoda.core.compatibility;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public enum MethodMapping {
    MC_ITEM_STACK__GET_TAG("getTag", "getTag", "s", "t", "u"),
    MC_ITEM_STACK__SET_TAG("setTag", "setTag", "c", "c", "c", ClassMapping.NBT_TAG_COMPOUND.getClazz()),

    MC_NBT_TAG_COMPOUND__SET("set", "set", "a", "a", "a", String.class, ClassMapping.NBT_BASE.getClazz()),
    MC_NBT_TAG_COMPOUND__REMOVE("remove", "remove", "r", "r", "r", String.class),

    CB_ITEM_STACK__AS_NMS_COPY("asNMSCopy", ItemStack.class),
    CB_ITEM_STACK__AS_CRAFT_MIRROR("asCraftMirror", ClassMapping.ITEM_STACK.getClazz())

    /* #updateNeighbourForOutputSignal */

    /* #setWarningBlocks */
    /* #lerpSizeBetween */;

    private final String saneFallback;
    private final String _1_14;
    private final String _1_17;
    private final String _1_18;
    private final String _1_18_2;
    private final String _1_19;
    private final Class<?>[] parameters;

    MethodMapping(String saneFallback, String _1_17, String _1_18, String _1_18_2, String _1_19, Class<?>... parameters) {
        this.saneFallback = saneFallback;

        this._1_14 = null;
        this._1_17 = _1_17;
        this._1_18 = _1_18;
        this._1_18_2 = _1_18_2;
        this._1_19 = _1_19;
        this.parameters = parameters;
    }

    MethodMapping(String saneFallback, String _1_18, String _1_18_2, String _1_19, Class<?>... parameters) {
        this.saneFallback = saneFallback;

        this._1_14 = null;
        this._1_17 = null;
        this._1_18 = _1_18;
        this._1_18_2 = _1_18_2;
        this._1_19 = _1_19;
        this.parameters = parameters;
    }

    MethodMapping(String saneFallback, Class<?>... parameters) {
        this.saneFallback = saneFallback;

        this._1_14 = null;
        this._1_17 = null;
        this._1_18 = null;
        this._1_18_2 = null;
        this._1_19 = null;
        this.parameters = parameters;
    }

    public Method getMethod(Class<?> clazz) {
        try {
            String methodName = _1_18;
            switch (ServerVersion.getServerVersion()) {
                case V1_14:
                    if (_1_14 != null) {
                        methodName = _1_14;
                    }

                    break;
                case V1_17:
                    if (_1_17 != null) {
                        methodName = _1_17;
                    }

                    break;
                case V1_18:
                    if (_1_18_2 != null) {
                        methodName = _1_18_2;
                    }

                    break;
                case V1_19:
                    if (_1_19 != null) {
                        methodName = _1_19;
                    }

                    break;
            }

            try {
                Method method = clazz.getMethod(methodName, parameters);
                method.setAccessible(true);

                return method;
            } catch (NullPointerException | NoSuchMethodException ex) {
                if (saneFallback != null && !saneFallback.equals(methodName)) {
                    try {
                        Method method = clazz.getMethod(saneFallback, parameters);
                        method.setAccessible(true);

                        return method;
                    } catch (NoSuchMethodException innerEx) {
                        ex.printStackTrace();
                        innerEx.printStackTrace();
                    }
                } else {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
