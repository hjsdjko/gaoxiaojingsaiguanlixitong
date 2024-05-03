
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 评审打分
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/pingshenPingfen")
public class PingshenPingfenController {
    private static final Logger logger = LoggerFactory.getLogger(PingshenPingfenController.class);

    private static final String TABLE_NAME = "pingshenPingfen";

    @Autowired
    private PingshenPingfenService pingshenPingfenService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GonggaoService gonggaoService;//竞赛公告
    @Autowired
    private HuojiangService huojiangService;//获奖
    @Autowired
    private LaoshiService laoshiService;//老师
    @Autowired
    private PingshenService pingshenService;//评审
    @Autowired
    private PingshenFenpeiService pingshenFenpeiService;//评审分配
    @Autowired
    private SaishiService saishiService;//赛事
    @Autowired
    private SaishiTijiaoService saishiTijiaoService;//赛事提交
    @Autowired
    private SaishiYuyueService saishiYuyueService;//赛事报名
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private ZhuanjiaService zhuanjiaService;//专家
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("老师".equals(role))
            params.put("laoshiId",request.getSession().getAttribute("userId"));
        else if("专家".equals(role))
            params.put("zhuanjiaId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = pingshenPingfenService.queryPage(params);

        //字典表数据转换
        List<PingshenPingfenView> list =(List<PingshenPingfenView>)page.getList();
        for(PingshenPingfenView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        PingshenPingfenEntity pingshenPingfen = pingshenPingfenService.selectById(id);
        if(pingshenPingfen !=null){
            //entity转view
            PingshenPingfenView view = new PingshenPingfenView();
            BeanUtils.copyProperties( pingshenPingfen , view );//把实体数据重构到view中
            //级联表 评审分配
            //级联表
            PingshenFenpeiEntity pingshenFenpei = pingshenFenpeiService.selectById(pingshenPingfen.getPingshenFenpeiId());
            if(pingshenFenpei != null){
            BeanUtils.copyProperties( pingshenFenpei , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "zhuanjiaId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setPingshenFenpeiId(pingshenFenpei.getId());
            }
            //级联表 专家
            //级联表
            ZhuanjiaEntity zhuanjia = zhuanjiaService.selectById(pingshenPingfen.getZhuanjiaId());
            if(zhuanjia != null){
            BeanUtils.copyProperties( zhuanjia , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "zhuanjiaId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setZhuanjiaId(zhuanjia.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody PingshenPingfenEntity pingshenPingfen, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,pingshenPingfen:{}",this.getClass().getName(),pingshenPingfen.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("专家".equals(role))
            pingshenPingfen.setZhuanjiaId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<PingshenPingfenEntity> queryWrapper = new EntityWrapper<PingshenPingfenEntity>()
            .eq("pingshen_fenpei_id", pingshenPingfen.getPingshenFenpeiId())
            .eq("zhuanjia_id", pingshenPingfen.getZhuanjiaId())
            .eq("pingshen_pingfen_pingfen", pingshenPingfen.getPingshenPingfenPingfen())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        PingshenPingfenEntity pingshenPingfenEntity = pingshenPingfenService.selectOne(queryWrapper);
        if(pingshenPingfenEntity==null){
            pingshenPingfen.setInsertTime(new Date());
            pingshenPingfen.setCreateTime(new Date());
            pingshenPingfenService.insert(pingshenPingfen);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody PingshenPingfenEntity pingshenPingfen, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,pingshenPingfen:{}",this.getClass().getName(),pingshenPingfen.toString());
        PingshenPingfenEntity oldPingshenPingfenEntity = pingshenPingfenService.selectById(pingshenPingfen.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("专家".equals(role))
//            pingshenPingfen.setZhuanjiaId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

            pingshenPingfenService.updateById(pingshenPingfen);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<PingshenPingfenEntity> oldPingshenPingfenList =pingshenPingfenService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        pingshenPingfenService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<PingshenPingfenEntity> pingshenPingfenList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            PingshenPingfenEntity pingshenPingfenEntity = new PingshenPingfenEntity();
//                            pingshenPingfenEntity.setPingshenPingfenUuidNumber(data.get(0));                    //报名唯一编号 要改的
//                            pingshenPingfenEntity.setPingshenFenpeiId(Integer.valueOf(data.get(0)));   //评审分配 要改的
//                            pingshenPingfenEntity.setZhuanjiaId(Integer.valueOf(data.get(0)));   //专家 要改的
//                            pingshenPingfenEntity.setPingshenPingfenPingfen(Integer.valueOf(data.get(0)));   //打分 要改的
//                            pingshenPingfenEntity.setPingshenPingfenText(data.get(0));                    //内容 要改的
//                            pingshenPingfenEntity.setInsertTime(date);//时间
//                            pingshenPingfenEntity.setCreateTime(date);//时间
                            pingshenPingfenList.add(pingshenPingfenEntity);


                            //把要查询是否重复的字段放入map中
                                //报名唯一编号
                                if(seachFields.containsKey("pingshenPingfenUuidNumber")){
                                    List<String> pingshenPingfenUuidNumber = seachFields.get("pingshenPingfenUuidNumber");
                                    pingshenPingfenUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> pingshenPingfenUuidNumber = new ArrayList<>();
                                    pingshenPingfenUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("pingshenPingfenUuidNumber",pingshenPingfenUuidNumber);
                                }
                        }

                        //查询是否重复
                         //报名唯一编号
                        List<PingshenPingfenEntity> pingshenPingfenEntities_pingshenPingfenUuidNumber = pingshenPingfenService.selectList(new EntityWrapper<PingshenPingfenEntity>().in("pingshen_pingfen_uuid_number", seachFields.get("pingshenPingfenUuidNumber")));
                        if(pingshenPingfenEntities_pingshenPingfenUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(PingshenPingfenEntity s:pingshenPingfenEntities_pingshenPingfenUuidNumber){
                                repeatFields.add(s.getPingshenPingfenUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [报名唯一编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        pingshenPingfenService.insertBatch(pingshenPingfenList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = pingshenPingfenService.queryPage(params);

        //字典表数据转换
        List<PingshenPingfenView> list =(List<PingshenPingfenView>)page.getList();
        for(PingshenPingfenView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        PingshenPingfenEntity pingshenPingfen = pingshenPingfenService.selectById(id);
            if(pingshenPingfen !=null){


                //entity转view
                PingshenPingfenView view = new PingshenPingfenView();
                BeanUtils.copyProperties( pingshenPingfen , view );//把实体数据重构到view中

                //级联表
                    PingshenFenpeiEntity pingshenFenpei = pingshenFenpeiService.selectById(pingshenPingfen.getPingshenFenpeiId());
                if(pingshenFenpei != null){
                    BeanUtils.copyProperties( pingshenFenpei , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setPingshenFenpeiId(pingshenFenpei.getId());
                }
                //级联表
                    ZhuanjiaEntity zhuanjia = zhuanjiaService.selectById(pingshenPingfen.getZhuanjiaId());
                if(zhuanjia != null){
                    BeanUtils.copyProperties( zhuanjia , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setZhuanjiaId(zhuanjia.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody PingshenPingfenEntity pingshenPingfen, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,pingshenPingfen:{}",this.getClass().getName(),pingshenPingfen.toString());
        Wrapper<PingshenPingfenEntity> queryWrapper = new EntityWrapper<PingshenPingfenEntity>()
            .eq("pingshen_pingfen_uuid_number", pingshenPingfen.getPingshenPingfenUuidNumber())
            .eq("pingshen_fenpei_id", pingshenPingfen.getPingshenFenpeiId())
            .eq("zhuanjia_id", pingshenPingfen.getZhuanjiaId())
            .eq("pingshen_pingfen_pingfen", pingshenPingfen.getPingshenPingfenPingfen())
            .eq("pingshen_pingfen_text", pingshenPingfen.getPingshenPingfenText())
//            .notIn("pingshen_pingfen_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        PingshenPingfenEntity pingshenPingfenEntity = pingshenPingfenService.selectOne(queryWrapper);
        if(pingshenPingfenEntity==null){
            pingshenPingfen.setInsertTime(new Date());
            pingshenPingfen.setCreateTime(new Date());
        pingshenPingfenService.insert(pingshenPingfen);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

