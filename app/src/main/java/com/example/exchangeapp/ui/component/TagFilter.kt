package com.example.exchangeapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.ItemCategory
import com.example.exchangeapp.ui.theme.ExchangeAppTheme

/**
 * TagFilter组件 - 标签筛选器
 *
 * **Validates: Requirements 14.4, 14.5** - 允许User按Item_Tag筛选Item，
 * 当User选择Item_Tag筛选时仅显示包含该Item_Tag的Item。
 *
 * 功能:
 * 1. 以横向可滚动的筛选芯片(FilterChip)展示所有预定义分类标签
 * 2. 提供"全部"选项用于清除筛选
 * 3. 高亮当前选中的标签
 * 4. 点击标签时通过回调通知调用方更新筛选条件
 *
 * 设计说明:
 * - 标签来源于 [ItemCategory.displayNames]，保证与预定义分类体系一致 (Requirement 14.1, 14.2)
 * - 选中已选标签会再次触发回调，由调用方决定是否取消筛选；"全部"芯片传回 null 以清除筛选
 *
 * @param selectedTag 当前选中的标签，null 表示未筛选（显示全部）
 * @param onTagSelected 标签选择回调，参数为选中的标签，null 表示清除筛选
 * @param modifier Modifier修饰符
 * @param tags 可供筛选的标签列表，默认为全部预定义分类
 */
@Composable
fun TagFilter(
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = ItemCategory.displayNames
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // "全部"选项，用于清除筛选
        item {
            FilterChip(
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
                label = { Text(text = "全部") }
            )
        }

        // 预定义分类标签
        items(tags) { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = {
                    // 再次点击已选中的标签会清除筛选，否则切换到该标签
                    onTagSelected(if (selectedTag == tag) null else tag)
                },
                label = { Text(text = tag) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagFilterUnselectedPreview() {
    ExchangeAppTheme {
        TagFilter(
            selectedTag = null,
            onTagSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagFilterSelectedPreview() {
    ExchangeAppTheme {
        TagFilter(
            selectedTag = "电子产品",
            onTagSelected = {}
        )
    }
}
