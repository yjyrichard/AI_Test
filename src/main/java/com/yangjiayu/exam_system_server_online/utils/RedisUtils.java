package com.yangjiayu.exam_system_server_online.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 封装RedisTemplate操作，提供更便捷的API
 */
@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并设置过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 设置过期时间
     * @param key 缓存键
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public Boolean expire(String key, long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 根据前缀获取所有匹配的key
     * @param pattern key前缀
     * @return 匹配的key集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 根据前缀删除所有匹配的key
     * @param pattern key前缀
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 设置Hash缓存
     * @param key 缓存键
     * @param hashKey Hash键
     * @param value Hash值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取Hash缓存
     * @param key 缓存键
     * @param hashKey Hash键
     * @return Hash值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 设置整个Hash缓存
     * @param key 缓存键
     * @param map Hash表
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取整个Hash缓存
     * @param key 缓存键
     * @return Hash表
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除Hash缓存中的某个键
     * @param key 缓存键
     * @param hashKey Hash键
     */
    public void hDelete(String key, Object... hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 判断Hash缓存中是否存在某个键
     * @param key 缓存键
     * @param hashKey Hash键
     * @return 是否存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 将列表放入缓存
     * @param key 缓存键
     * @param value 列表值
     * @return 列表长度
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 将列表放入缓存
     * @param key 缓存键
     * @param value 列表值
     * @param timeout 过期时间（秒）
     * @return 列表长度
     */
    public Long lPush(String key, Object value, long timeout) {
        Long count = redisTemplate.opsForList().rightPush(key, value);
        expire(key, timeout);
        return count;
    }

    /**
     * 将多个值放入列表缓存
     * @param key 缓存键
     * @param values 值列表
     * @return 列表长度
     */
    public Long lPushAll(String key, List<Object> values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 将多个值放入列表缓存并设置过期时间
     * @param key 缓存键
     * @param values 值列表
     * @param timeout 过期时间（秒）
     * @return 列表长度
     */
    public Long lPushAll(String key, List<Object> values, long timeout) {
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        expire(key, timeout);
        return count;
    }

    /**
     * 获取列表缓存
     * @param key 缓存键
     * @param start 开始索引
     * @param end 结束索引
     * @return 列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     * @param key 缓存键
     * @return 列表长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 获取列表中指定索引的值
     * @param key 缓存键
     * @param index 索引
     * @return 值
     */
    public Object lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 移除列表中的值
     * @param key 缓存键
     * @param count 移除数量
     * @param value 值
     * @return 移除数量
     */
    public Long lRemove(String key, long count, Object value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }
    
    /**
     * 向有序集合添加元素，如果已存在则更新分数
     * @param key 缓存键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }
    
    /**
     * 增加有序集合中元素的分数
     * @param key 缓存键
     * @param value 值
     * @param delta 增加的分数
     * @return 新的分数
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }
    
    /**
     * 获取有序集合中元素的分数
     * @param key 缓存键
     * @param value 值
     * @return 分数
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }
    
    /**
     * 获取有序集合的大小
     * @param key 缓存键
     * @return 集合大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }
    
    /**
     * 获取有序集合中指定分数范围的元素
     * @param key 缓存键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }
    
    /**
     * 获取有序集合中指定排名范围的元素（从高到低）
     * @param key 缓存键
     * @param start 开始排名（0表示第一个）
     * @param end 结束排名
     * @return 元素集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }
    
    /**
     * 获取有序集合中指定排名范围的元素和分数（从高到低）
     * @param key 缓存键
     * @param start 开始排名（0表示第一个）
     * @param end 结束排名
     * @return 元素和分数的集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }
    
    /**
     * 移除有序集合中的元素
     * @param key 缓存键
     * @param values 要移除的元素
     * @return 移除的数量
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }
}