package my.cute.channelpoints;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelJoinEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

public class MyEventListener {

	/*
	 * TODO
	 * use a caffeine cache per listened channel (only have 1 anyway but yea)
	 * on channel message, get channel's cache, add author to cache
	 * evict entries from cache after some time
	 * when require random user, get arbitrary user from cache. result is any user who
	 * has talked in the past x time (determined by cache eviction time)
	 */
	private final TwitchClient client;
	/*
	 * we don't really have a use for values in the cache?
	 * like we just care about holding usernames. there's no corresponding value
	 * other than like, the time they last talked, which is automatically managed
	 * internally by the cache. currently just using dummy value. better way to
	 * do this?
	 */
	private final Map<String, Cache<String, Boolean>> userCaches;
	
	MyEventListener(TwitchClient client) {
		this.client = client;
		this.userCaches = new ConcurrentHashMap<>();
	}
	
	/*
	 * see: Bot.Bot() for the suppresswarnings stuff
	 */
	@SuppressWarnings("resource")
	@EventSubscriber
	public void onChannelMessageEvent(ChannelMessageEvent event) {
		System.out.println(event.getMessage());
		client.getChat().sendMessage(event.getChannel().getName(), event.getMessage() + ".");
		this.registerUser(event.getChannel().getName(), event.getUser().getName());
	}
	
	@EventSubscriber
	public void onRewardRedeemedEvent(RewardRedeemedEvent event) {
		System.out.println("reward id: " + event.getRedemption().getReward().getId() + ", title: " + event.getRedemption().getReward().getTitle());
	}
	
	@EventSubscriber
	public void onChannelJoinEvent(ChannelJoinEvent event) {
		Caffeine<Object, Object> builder = Caffeine.newBuilder()
				.expireAfterAccess(45, TimeUnit.MINUTES)
				.maximumSize(10_000)
				.scheduler(Scheduler.systemScheduler());
		this.userCaches.put(event.getChannel().getName(), builder.<String, Boolean>build());
	}
	
	/**
	 * add the given username into the cache for the given channelname.
	 * if they already exist in the cache, do nothing
	 * @param channelName the channelname for the cache to add the user to
	 * @param userName the user to add to the cache
	 */
	private void registerUser(String channelName, String userName) {
		this.userCaches.get(channelName).get(userName, name -> true);
	}
}
