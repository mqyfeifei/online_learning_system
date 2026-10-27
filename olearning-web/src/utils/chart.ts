import * as echarts from 'echarts'

/** 图表统一色板：与 theme.css 的暖米橙主色保持一致 */
export const CHART_COLORS = [
  '#ff6b35',
  '#f0a52c',
  '#7c6cf5',
  '#2fa36b',
  '#3b82f6',
  '#e8737a',
  '#c48b5c',
  '#8e7cc3'
]

/** 分数段固定配色：高分段绿、低分段红，不随数据顺序变化 */
export const SCORE_COLORS: Record<string, string> = {
  '90-100': '#2fa36b',
  '80-89': '#7cc79b',
  '70-79': '#f0a52c',
  '60-69': '#f6c98a',
  '0-59': '#e5533d'
}

const AXIS_LABEL = { color: '#8d847a', fontSize: 11 }
const SPLIT_LINE = { lineStyle: { color: '#f0e7dc', type: 'dashed' as const } }

/** 统一样式的柱状图配置 */
export function barOption(
  categories: string[],
  values: number[],
  color = CHART_COLORS[0],
  name = ''
) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 12, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: { ...AXIS_LABEL, interval: 0, rotate: categories.length > 5 ? 18 : 0 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#f0e7dc' } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: AXIS_LABEL,
      splitLine: SPLIT_LINE,
      axisLine: { show: false }
    },
    series: [
      {
        name,
        type: 'bar',
        data: values,
        barMaxWidth: 34,
        itemStyle: { color, borderRadius: [8, 8, 0, 0] }
      }
    ]
  } as echarts.EChartsOption
}

/** 统一样式的环形图配置 */
export function donutOption(
  data: { name: string; value: number; itemStyle?: { color: string } }[],
  unit = '门'
) {
  return {
    tooltip: { trigger: 'item', formatter: `{b}: {c} ${unit} ({d}%)` },
    legend: {
      bottom: 0,
      itemWidth: 8,
      itemHeight: 8,
      icon: 'circle',
      textStyle: { ...AXIS_LABEL, fontSize: 11.5 }
    },
    color: CHART_COLORS,
    series: [
      {
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 700, formatter: '{b}\n{c}' } },
        data
      }
    ]
  } as echarts.EChartsOption
}

/** 初始化并绑定容器尺寸变化，返回实例便于后续 setOption */
export function mountChart(el: HTMLElement, option: echarts.EChartsOption) {
  const chart = echarts.init(el)
  chart.setOption(option)
  const onResize = () => chart.resize()
  window.addEventListener('resize', onResize)
  return chart
}
