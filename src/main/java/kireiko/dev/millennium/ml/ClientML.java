package kireiko.dev.millennium.ml;

import kireiko.dev.millennium.ml.data.module.ModuleML;
import kireiko.dev.millennium.ml.logic.Logger;
import kireiko.dev.millennium.ml.logic.ModelVer;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ClientML {

    public static final boolean DEV_MODE = false;

    public static final String CLIENT_NAME = "001-spartan-complex";
    private static final int TABLE_SIZE = 2;

    public static final List<ModuleML> MODEL_LIST = Arrays.asList(

    );

    public void run() {
        if (DEV_MODE) {
            for (int i = 0; i < MODEL_LIST.size(); i++) {
                final ModuleML moduleML = MODEL_LIST.get(i);
                if (DEV_MODE) {
                    FactoryML.loadFromFile(i, moduleML.getName() + ".dat", TABLE_SIZE, moduleML.getParameterBuffer(), ModelVer.VERSION_4_5);
                } else {
                    FactoryML.loadFromResources(i, MODEL_LIST.get(i).getName() + ".dat", TABLE_SIZE, moduleML.getParameterBuffer(), ModelVer.VERSION_4_5);
                }
            }
        }
        Logger.info(CLIENT_NAME + " loaded!");
    }
}
