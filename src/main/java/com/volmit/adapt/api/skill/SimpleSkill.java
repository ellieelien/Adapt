package com.volmit.adapt.api.skill;

import com.google.gson.Gson;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import com.volmit.adapt.api.tick.TickedObject;
import com.volmit.adapt.api.world.AdaptStatTracker;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.FileWatcher;
import com.volmit.adapt.util.IO;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.JSONObject;
import com.volmit.adapt.util.KList;
import com.volmit.adapt.util.advancements.advancement.AdvancementVisibility;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
public abstract class SimpleSkill<T> extends TickedObject implements Skill<T> {
    private final String name;
    private final String emojiName;
    private C color;
    private double minXp;
    private String description;
    private Material icon;
    @EqualsAndHashCode.Exclude
    private KList<Adaptation<?>> adaptations;
    private KList<AdaptStatTracker> statTrackers;
    private KList<AdaptAdvancement> cachedAdvancements;
    private String advancementBackground;
    private KList<AdaptRecipe> recipes;
    private Class<T> configType;
    private T config;

    public SimpleSkill(String name, String emojiName) {
        super("skill", UUID.randomUUID() + "-skill-" + name, 50);
        statTrackers = new KList<>();
        recipes = new KList<>();
        cachedAdvancements = new KList<>();
        this.emojiName = emojiName;
        adaptations = new KList<>();
        setColor(C.WHITE);
        this.name = name;
        setIcon(Material.BOOK);
        setDescription("No Description Provided");
        setMinXp(100);
        setAdvancementBackground("minecraft:textures/block/deepslate_tiles.png");

        J.a(() -> {
            J.attempt(this::getConfig);
            getAdaptations().forEach(i -> J.attempt(i::getConfig));
        }, 1);
    }

    @Override
    public Class<T> getConfigurationClass() {
        return configType;
    }

    @Override
    public void registerConfiguration(Class<T> type) {
        this.configType = type;
        File file = Adapt.instance.getDataFile("adapt", "skills", getName() + ".json");
        FileWatcher fw = new FileWatcher(file);
        fw.checkModified();
        J.a(() -> {
            fw.checkModified();
            Adapt.instance.getTicker().register(new TickedObject("config", "config-" + getName(), 1000) {
                @Override
                public void onTick() {
                    if(fw.checkModified() && file.exists())
                    {
                        config = null;
                        getConfig();
                        Adapt.info("Hotloaded " + file.getPath());
                        fw.checkModified();
                    }
                }
            });
        }, 20);
    }

    @Override
    public T getConfig() {
        try {
            if(config == null) {
                T dummy = getConfigurationClass().getConstructor().newInstance();
                File l = Adapt.instance.getDataFile("adapt", "skills", getName() + ".json");

                if(!l.exists()) {
                    try {
                        IO.writeAll(l, new JSONObject(new Gson().toJson(dummy)).toString(4));
                    } catch(IOException e) {
                        e.printStackTrace();
                        config = dummy;
                        return config;
                    }
                }

                try {
                    config = new Gson().fromJson(IO.readAll(l), getConfigurationClass());
                    IO.writeAll(l, new JSONObject(new Gson().toJson(config)).toString(4));
                } catch(IOException e) {
                    e.printStackTrace();
                    config = dummy;
                    return config;
                }
            }
        } catch(Throwable e) {

        }

        return config;
    }

    public void registerRecipe(AdaptRecipe r) {
        recipes.add(r);
    }

    public void registerAdvancement(AdaptAdvancement a) {
        cachedAdvancements.add(a);
    }

    @Override
    public void onRegisterAdvancements(KList<AdaptAdvancement> advancements) {
        advancements.addAll(cachedAdvancements);
    }

    public AdaptAdvancement buildAdvancements() {
        KList<AdaptAdvancement> a = new KList<>();
        onRegisterAdvancements(a);

        for(Adaptation<?> i : getAdaptations()) {
            a.add(i.buildAdvancements());
        }

        return AdaptAdvancement.builder()
            .background(getAdvancementBackground())
            .key("skill_" + getName())
            .title(getDisplayName())
            .description(getDescription())
            .icon(getIcon())
            .children(a)
            .visibility(AdvancementVisibility.HIDDEN)
            .build();
    }

    @Override
    public void registerStatTracker(AdaptStatTracker tracker) {
        getStatTrackers().add(tracker);
    }

    @Override
    public KList<AdaptStatTracker> getStatTrackers() {
        return statTrackers;
    }

    @Override
    public void registerAdaptation(Adaptation<?> a) {
        if(!a.isEnabled()) {
            return;
        }

        a.setSkill(this);
        adaptations.add(a);
    }

    @Override
    public void unregister() {
        adaptations.forEach(Adaptation::unregister);
    }

    @Override
    public abstract void onTick();
}
