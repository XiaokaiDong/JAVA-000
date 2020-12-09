package jdbc.sharding.sql.impl;

import jdbc.sharding.sql.SqlProcessor;

public class SimpleSqlProcessor implements SqlProcessor {
    @Override
    public Object getShardingKey(String sql, String shardingKey, Object[] args) {
        int firstIndex = 0;
        int lastIndex = 0;
        int shardingKeyIndex = 0;

        String subString = null;

        Object shardingKeyObj = null;

        sql.toUpperCase();

        //1、先处理INSERT语句，因为INSERT语句可能不出现shardingKey所在的列。这里做简化，只处理包含列名的语句
        if (sql.indexOf("INSERT") != -1) {
            firstIndex = sql.indexOf("(");
            lastIndex = sql.indexOf(")");
            subString = sql.substring(firstIndex + 1, lastIndex).replaceAll("\\s*", "");
            String[] columns = subString.split(",");
            for (shardingKeyIndex = 0; shardingKeyIndex < columns.length; shardingKeyIndex ++) {
                if (columns[shardingKeyIndex].equals(shardingKey)) {
                    break;
                }
            }
            if (shardingKeyIndex == columns.length) {
                return null;
            } else {
                return args[shardingKeyIndex];
            }
        }

        //2、处理UPDATE、DELETE、SELECT，在WHERE后面寻找
        if (sql.indexOf("SELECT") != -1 || sql.indexOf("DELETE") != -1 || sql.indexOf("UPDATE") != -1) {
            //寻找WHERE
            firstIndex = sql.indexOf("WHERE");
            if(firstIndex != -1) {
                //在WHERE后寻找分区键
                lastIndex = sql.indexOf(shardingKey, firstIndex);
                //把分区键后的内容按照等号分割, 结果数组中第2个元素就包含了分区键的值
                String[] conditions = sql.substring(lastIndex).split("=");

                //记住分区键后的第一个"？"的位置，后面使用
                int indexTmp = sql.indexOf('?', lastIndex);

                //分割出分区键,按空格分割，第一个元素就是分区键
                String shardingKeyName = conditions[1].trim().split("\\s")[0];
                if (!shardingKeyName.equals("?")) {
                    //如果条件是常量
                    return shardingKeyName;
                } else {
                    //得到分区键值的序号
                    if (indexTmp == -1){
                        //SQL语句中不包含?，语句有问题
                        return null;
                    } else {
                        //找到SQL字符串中第一个"?"
                        firstIndex = sql.indexOf('?');
                        subString = sql.substring(firstIndex, indexTmp + 1);
                        shardingKeyIndex = 0;
                        firstIndex = 0;
                        while((firstIndex = subString.indexOf('?', firstIndex)) != -1) {
                            shardingKeyIndex ++;
                        }
                        return args[shardingKeyIndex];
                    }
                }
            } else {
                return null;
            }
        }

        return null;
    }

}
