package com.boke.db.service.impl;


import com.boke.db.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Redis服务实现类
 *
 *  封装了Redis的各种数据类型操作，包括：
 *  * - String（字符串）
 *  * - Hash（哈希）
 *  * - List（列表）
 *  * - Set（集合）
 *  * - ZSet（有序集合）
 *  * - Geo（地理位置）
 *  * - HyperLogLog（基数统计）
 *  * - Bitmap（位图）
 *
 * set(key, value, time) - 设置带过期时间的缓存
 * set(key, value) - 设置永久缓存
 * get(key) - 获取缓存值
 * del(key) - 删除单个缓存
 * del(keys) - 批量删除缓存
 * expire(key, time) - 设置过期时间
 * getExpire(key) - 获取过期时间
 * hasKey(key) - 判断key是否存在
 *
 * incr(key, delta) - 递增
 * incrExpire(key, time) - 递增并设置过期时间
 * decr(key, delta) - 递减
 *
 * hGet(key, hashKey) - 获取Hash中的数据
 * hSet(key, hashKey, value, time) - 设置Hash数据带过期时间
 * hSet(key, hashKey, value) - 设置Hash数据
 * hGetAll(key) - 获取所有Hash数据
 * hSetAll(key, map, time) - 设置多个Hash数据带过期时间
 * hSetAll(key, map) - 设置多个Hash数据
 * hDel(key, hashKey) - 删除Hash数据
 * hHasKey(key, hashKey) - 判断Hash数据是否存在
 * hIncr(key, hashKey, delta) - Hash递增
 * hDecr(key, hashKey, delta) - Hash递减
 *
 * zIncr(key, value, score) - 增加分数
 * zDecr(key, value, score) - 减少分数
 * zReverseRangeWithScore(key, start, end) - 获取指定范围的有序集合（带分数）
 * zScore(key, value) - 获取分数
 * zAllScore(key) - 获取所有分数
 *
 * sMembers(key) - 获取集合所有成员
 * sAdd(key, values) - 添加集合成员
 * sAddExpire(key, time, values) - 添加集合成员并设置过期时间
 * sIsMember(key, value) - 判断是否为集合成员
 * sSize(key) - 获取集合大小
 * sRemove(key, values) - 移除集合成员
 *
 * lRange(key, start, end) - 获取列表指定范围的元素
 * lSize(key) - 获取列表长度
 * lIndex(key, index) - 获取指定索引的元素
 * lPush(key, value) - 从右侧推入元素
 * lPush(key, value, time) - 从右侧推入元素并设置过期时间
 * lPushAll(key, values) - 从右侧推入多个元素
 * lPushAll(key, time, values) - 从右侧推入多个元素并设置过期时间
 * lRemove(key, count, value) - 删除列表元素
 *
 * bitAdd(key, offset, b) - 设置位图的值
 * bitGet(key, offset) - 获取位图的值
 * bitCount(key) - 获取位图中值为1的个数
 * bitField(key, limit, offset) - 获取位图指定范围的值
 * bitGetAll(key) - 获取位图的所有值
 *
 * hyperAdd(key, value) - 添加数据
 * hyperGet(key) - 统计数据
 * hyperDel(key) - 删除数据
 *
 * geoAdd(key, x, y, name) - 添加地理位置
 * geoGetPointList(key, place) - 获取地理位置
 * geoCalculationDistance(key, placeOne, placeTow) - 计算两个位置之间的距离
 * geoNearByPlace(key, place, distance, limit, sort) - 查询指定范围内的位置
 * geoGetHash(key, place) - 获取地理位置的geohash值
 *
 * @author boke
 * @since 1.0
 */

@Service
@SuppressWarnings("all")
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object value, long time) {
        redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
    }

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public Long del(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    public Boolean expire(String key, long time) {
        return redisTemplate.expire(key, time, TimeUnit.SECONDS);
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public Long incr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long incrExpire(String key, long time) {
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count != null && count == 1) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
        }
        return count;
    }

    @Override
    public Long decr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    @Override
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public Boolean hSet(String key, String hashKey, Object value, long time) {
        redisTemplate.opsForHash().put(key, hashKey, value);
        return expire(key, time);
    }

    @Override
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public Map hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public Boolean hSetAll(String key, Map<String, Object> map, long time) {
        redisTemplate.opsForHash().putAll(key, map);
        return expire(key, time);
    }

    @Override
    public void hSetAll(String key, Map<String, ?> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public void hDel(String key, Object... hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    @Override
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    @Override
    public Long hIncr(String key, String hashKey, Long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    @Override
    public Long hDecr(String key, String hashKey, Long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, -delta);
    }

    @Override
    public Double zIncr(String key, Object value, Double score) {
        return redisTemplate.opsForZSet().incrementScore(key, value, score);
    }

    @Override
    public Double zDecr(String key, Object value, Double score) {
        return redisTemplate.opsForZSet().incrementScore(key, value, -score);
    }

    @Override
    public Map<Object, Double> zReverseRangeWithScore(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end)
                .stream()
                .collect(Collectors.toMap(ZSetOperations.TypedTuple::getValue, ZSetOperations.TypedTuple::getScore));
    }

    @Override
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Map<Object, Double> zAllScore(String key) {
        return Objects.requireNonNull(redisTemplate.opsForZSet().rangeWithScores(key, 0, -1))
                .stream()
                .collect(Collectors.toMap(ZSetOperations.TypedTuple::getValue, ZSetOperations.TypedTuple::getScore));
    }

    @Override
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Long sAddExpire(String key, long time, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        expire(key, time);
        return count;
    }

    @Override
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    @Override
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    @Override
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @Override
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    public Object lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    @Override
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public Long lPush(String key, Object value, long time) {
        Long index = redisTemplate.opsForList().rightPush(key, value);
        expire(key, time);
        return index;
    }

    @Override
    public Long lPushAll(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    @Override
    public Long lPushAll(String key, Long time, Object... values) {
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        expire(key, time);
        return count;
    }

    @Override
    public Long lRemove(String key, long count, Object value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    @Override
    public Boolean bitAdd(String key, int offset, boolean b) {
        return redisTemplate.opsForValue().setBit(key, offset, b);
    }

    @Override
    public Boolean bitGet(String key, int offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    @Override
    public Long bitCount(String key) {
        return redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes()));
    }

    @Override
    public List<Long> bitField(String key, int limit, int offset) {
        return redisTemplate.execute((RedisCallback<List<Long>>) con ->
                con.bitField(key.getBytes(),
                        BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(limit)).valueAt(offset)));
    }

    @Override
    public byte[] bitGetAll(String key) {
        return redisTemplate.execute((RedisCallback<byte[]>) con -> con.get(key.getBytes()));
    }

    @Override
    public Long hyperAdd(String key, Object... value) {
        return redisTemplate.opsForHyperLogLog().add(key, value);
    }

    @Override
    public Long hyperGet(String... key) {
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    @Override
    public void hyperDel(String key) {
        redisTemplate.opsForHyperLogLog().delete(key);
    }

    @Override
    public Long geoAdd(String key, Double x, Double y, String name) {
        return redisTemplate.opsForGeo().add(key, new Point(x, y), name);
    }

    @Override
    public List<Point> geoGetPointList(String key, Object... place) {
        return redisTemplate.opsForGeo().position(key, place);
    }

    @Override
    public Distance geoCalculationDistance(String key, String placeOne, String placeTow) {
        return redisTemplate.opsForGeo()
                .distance(key, placeOne, placeTow, RedisGeoCommands.DistanceUnit.KILOMETERS);
    }

    @Override
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoNearByPlace(String key, String place, Distance distance, long limit, Sort.Direction sort) {
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates();
        // 判断排序方式
        if (Sort.Direction.ASC == sort) {
            args.sortAscending();
        } else {
            args.sortDescending();
        }
        args.limit(limit);
        return redisTemplate.opsForGeo()
                .radius(key, place, distance, args);
    }

    @Override
    public List<String> geoGetHash(String key, String... place) {
        return redisTemplate.opsForGeo()
                .hash(key, place);
    }

}
