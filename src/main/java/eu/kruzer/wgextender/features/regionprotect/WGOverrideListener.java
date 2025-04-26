package eu.kruzer.wgextender.features.regionprotect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import eu.kruzer.wgextender.WGExtender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class WGOverrideListener implements Listener {

	private final List<Map.Entry<HandlerList, RegisteredListener>> overriddenEvents = new ArrayList<>();

	public void inject() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		for (Method method : getClass().getMethods()) {
			if (method.isAnnotationPresent(EventHandler.class)) {
				Class<?> eventClass = method.getParameterTypes()[0];
				HandlerList hl = (HandlerList) eventClass.getMethod("getHandlerList").invoke(null);
				for (RegisteredListener listener : new ArrayList<>(Arrays.asList(hl.getRegisteredListeners()))) {
					if (listener.getListener().getClass() == getClassToReplace()) {
						overriddenEvents.add(new AbstractMap.SimpleEntry<>(hl, listener));
						hl.unregister(listener);
					}
				}
			}
		}
		Bukkit.getPluginManager().registerEvents(this, WGExtender.getInstance());
	}

	public void uninject() {
		HandlerList.unregisterAll(this);
		for (Map.Entry<HandlerList, RegisteredListener> pair : overriddenEvents) {
			pair.getKey().register(pair.getValue());
		}
		overriddenEvents.clear();
	}

	protected abstract Class<? extends Listener> getClassToReplace();
}