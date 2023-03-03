import imp


'''
使用场景:
1. 批量请求 url 解析返回值 生成输出值
1.1 解析课程 meta 的 json. 生成 markdown [课程标题](课程主页链接) 的文字格式

使用说明:
1. url 列表应该由外部自己生成(url 可能有多个需要批量生成的地方,例如序号部分 1、2、3、4... 例如实体id部分 {xxxid1}、{xxxid2}、{xxxid3}...)
   外部批量生成写入文件然后读取会省去这一个步骤. 很方便
'''

import requests

# todo 从命令行参数读取
urlFile=""
cookie=""


with open(urlFile) as file:
    urlList = [line.rstrip() for line in file]

for urlItem in urlList:
    

# for 按行循环文件
# 1. 发起 curl 请求(外部传入 cookie、 url 列表(如果有一堆url应该是外部自己批量生成写到一个文件里的))
# 1.1 外部是否传入暂停x秒
# 2. 解析 json 取出某几个字段
# 3. 按格式拼接输出字段

