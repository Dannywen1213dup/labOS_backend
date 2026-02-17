package ${packageName}.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ${packageName}.common.ErrorCode;
import ${packageName}.constant.CommonConstant;
import ${packageName}.exception.ThrowUtils;
import ${packageName}.mapper.${upperDataKey}Mapper;
import ${packageName}.model.dto.${dataKey}.${upperDataKey}QueryRequest;
import ${packageName}.model.entity.${upperDataKey};
import ${packageName}.model.entity.${upperDataKey}Favour;
import ${packageName}.model.entity.${upperDataKey}Thumb;
import ${packageName}.model.entity.User;
import ${packageName}.model.vo.${upperDataKey}VO;
import ${packageName}.model.vo.UserVO;
import ${packageName}.service.${upperDataKey}Service;
import ${packageName}.service.UserService;
import ${packageName}.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ${dataName} service implementation
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 */
@Service
@Slf4j
public class ${upperDataKey}ServiceImpl extends ServiceImpl<${upperDataKey}Mapper, ${upperDataKey}> implements ${upperDataKey}Service {

    @Resource
    private UserService userService;

    /**
     * Validate data
     *
     * @param ${dataKey}
     * @param add      Validate data for creation
     */
    @Override
    public void valid${upperDataKey}(${upperDataKey} ${dataKey}, boolean add) {
        ThrowUtils.throwIf(${dataKey} == null, ErrorCode.PARAMS_ERROR);
        // todo Get values from object
        String title = ${dataKey}.getTitle();
        // When creating data, parameters cannot be empty
        if (add) {
            // todo Add validation rules
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // When updating data, validate if parameters exist
        // todo Add validation rules
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "Title too long");
        }
    }

    /**
     * Get query wrapper
     *
     * @param ${dataKey}QueryRequest
     * @return
     */
    @Override
    public QueryWrapper<${upperDataKey}> getQueryWrapper(${upperDataKey}QueryRequest ${dataKey}QueryRequest) {
        QueryWrapper<${upperDataKey}> queryWrapper = new QueryWrapper<>();
        if (${dataKey}QueryRequest == null) {
            return queryWrapper;
        }
        // todo Get values from object
        Long id = ${dataKey}QueryRequest.getId();
        Long notId = ${dataKey}QueryRequest.getNotId();
        String title = ${dataKey}QueryRequest.getTitle();
        String content = ${dataKey}QueryRequest.getContent();
        String searchText = ${dataKey}QueryRequest.getSearchText();
        String sortField = ${dataKey}QueryRequest.getSortField();
        String sortOrder = ${dataKey}QueryRequest.getSortOrder();
        List<String> tagList = ${dataKey}QueryRequest.getTags();
        Long userId = ${dataKey}QueryRequest.getUserId();
        // todo Add required query conditions
        // Search from multiple fields
        if (StringUtils.isNotBlank(searchText)) {
            // Need to concatenate query conditions
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // Fuzzy query
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON array query
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // Exact query
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // Sort rule
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * Get ${dataName} VO
     *
     * @param ${dataKey}
     * @param request
     * @return
     */
    @Override
    public ${upperDataKey}VO get${upperDataKey}VO(${upperDataKey} ${dataKey}, HttpServletRequest request) {
        // Convert entity to VO
        ${upperDataKey}VO ${dataKey}VO = ${upperDataKey}VO.objToVo(${dataKey});

        // todo Can supplement values for VO object as needed, delete unnecessary content
        // region Optional
        // 1. Query user information
        Long userId = ${dataKey}.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        ${dataKey}VO.setUser(userVO);
        // 2. If logged in, get user thumb and favor status
        long ${dataKey}Id = ${dataKey}.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // Get thumb
            QueryWrapper<${upperDataKey}Thumb> ${dataKey}ThumbQueryWrapper = new QueryWrapper<>();
            ${dataKey}ThumbQueryWrapper.in("${dataKey}Id", ${dataKey}Id);
            ${dataKey}ThumbQueryWrapper.eq("userId", loginUser.getId());
            ${upperDataKey}Thumb ${dataKey}Thumb = ${dataKey}ThumbMapper.selectOne(${dataKey}ThumbQueryWrapper);
            ${dataKey}VO.setHasThumb(${dataKey}Thumb != null);
            // Get favor
            QueryWrapper<${upperDataKey}Favour> ${dataKey}FavourQueryWrapper = new QueryWrapper<>();
            ${dataKey}FavourQueryWrapper.in("${dataKey}Id", ${dataKey}Id);
            ${dataKey}FavourQueryWrapper.eq("userId", loginUser.getId());
            ${upperDataKey}Favour ${dataKey}Favour = ${dataKey}FavourMapper.selectOne(${dataKey}FavourQueryWrapper);
            ${dataKey}VO.setHasFavour(${dataKey}Favour != null);
        }
        // endregion

        return ${dataKey}VO;
    }

    /**
     * Get ${dataName} VO page
     *
     * @param ${dataKey}Page
     * @param request
     * @return
     */
    @Override
    public Page<${upperDataKey}VO> get${upperDataKey}VOPage(Page<${upperDataKey}> ${dataKey}Page, HttpServletRequest request) {
        List<${upperDataKey}> ${dataKey}List = ${dataKey}Page.getRecords();
        Page<${upperDataKey}VO> ${dataKey}VOPage = new Page<>(${dataKey}Page.getCurrent(), ${dataKey}Page.getSize(), ${dataKey}Page.getTotal());
        if (CollUtil.isEmpty(${dataKey}List)) {
            return ${dataKey}VOPage;
        }
        // Entity list => VO list
        List<${upperDataKey}VO> ${dataKey}VOList = ${dataKey}List.stream().map(${dataKey} -> {
            return ${upperDataKey}VO.objToVo(${dataKey});
        }).collect(Collectors.toList());

        // todo Can supplement values for VO object as needed, delete unnecessary content
        // region Optional
        // 1. Query user information
        Set<Long> userIdSet = ${dataKey}List.stream().map(${upperDataKey}::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. If logged in, get user thumb and favor status
        Map<Long, Boolean> ${dataKey}IdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> ${dataKey}IdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> ${dataKey}IdSet = ${dataKey}List.stream().map(${upperDataKey}::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // Get thumbs
            QueryWrapper<${upperDataKey}Thumb> ${dataKey}ThumbQueryWrapper = new QueryWrapper<>();
            ${dataKey}ThumbQueryWrapper.in("${dataKey}Id", ${dataKey}IdSet);
            ${dataKey}ThumbQueryWrapper.eq("userId", loginUser.getId());
            List<${upperDataKey}Thumb> ${dataKey}${upperDataKey}ThumbList = ${dataKey}ThumbMapper.selectList(${dataKey}ThumbQueryWrapper);
            ${dataKey}${upperDataKey}ThumbList.forEach(${dataKey}${upperDataKey}Thumb -> ${dataKey}IdHasThumbMap.put(${dataKey}${upperDataKey}Thumb.get${upperDataKey}Id(), true));
            // Get favors
            QueryWrapper<${upperDataKey}Favour> ${dataKey}FavourQueryWrapper = new QueryWrapper<>();
            ${dataKey}FavourQueryWrapper.in("${dataKey}Id", ${dataKey}IdSet);
            ${dataKey}FavourQueryWrapper.eq("userId", loginUser.getId());
            List<${upperDataKey}Favour> ${dataKey}FavourList = ${dataKey}FavourMapper.selectList(${dataKey}FavourQueryWrapper);
            ${dataKey}FavourList.forEach(${dataKey}Favour -> ${dataKey}IdHasFavourMap.put(${dataKey}Favour.get${upperDataKey}Id(), true));
        }
        // Fill information
        ${dataKey}VOList.forEach(${dataKey}VO -> {
            Long userId = ${dataKey}VO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            ${dataKey}VO.setUser(userService.getUserVO(user));
            ${dataKey}VO.setHasThumb(${dataKey}IdHasThumbMap.getOrDefault(${dataKey}VO.getId(), false));
            ${dataKey}VO.setHasFavour(${dataKey}IdHasFavourMap.getOrDefault(${dataKey}VO.getId(), false));
        });
        // endregion

        ${dataKey}VOPage.setRecords(${dataKey}VOList);
        return ${dataKey}VOPage;
    }

}
