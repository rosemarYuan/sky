package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController    // @Controller + @ResponseBody,IOC容器Bean标识 + 返回值以JSON格式作为HTTP相应题返回，而不是直接解析为被跳转的页面
@RequestMapping("/admin/dish") // 处理请求的统一基础路径
@Api(tags="菜品相关接口") // Swagger在线文档生成
@Slf4j  // Slf4j，日志log生成。
public class DishController  {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品...:{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 精确清理：只清理该分类下的缓存
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    //请求参数是Quere，即？xx=xx，因此不用@RequestBody
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询,{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除菜品,{}",ids);
        dishService.deleteBatch(ids);

        // 缓存处理：由于一个菜品可能在多个分类中，简单来做把所有dish_全删除了就行
        // 后台管理系统来说，管理员删除菜品的操作频率不高，
        // 全量清理：所有以 dish_ 开头的key
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品,{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品,{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        // 全量清理：所有以 dish_ 开头的key
        cleanCache("dish_*");

        return Result.success();

    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品，...{}",categoryId);
        List<Dish> dishVOList = dishService.list(categoryId);
        return Result.success(dishVOList);
    }


    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品销售状态")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("根据分类id修改菜品销售状态：{}", status);
        dishService.updateStatusById(status,id);

        // 全量清理：所有以 dish_ 开头的key
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 清理缓存的通用方法
     * @param pattern Redis key的匹配模式，例如：dish_*
     */
    private void cleanCache(String pattern){

        log.info(" > 进行了一次缓存清理，此时Redis中的缓存发生了更新 < ");

        // 1. 获取所有匹配的 Key
        Set keys = redisTemplate.keys(pattern);

        // 2. 删除这些 Key
        redisTemplate.delete(keys);
    }

}
