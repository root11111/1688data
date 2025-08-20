// 爬虫管理系统JavaScript
class CrawlerManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 20;
        this.currentTaskId = null;
        this.init();
    }

    init() {
        this.loadStats();
        this.loadTasks();
        this.loadData();
        this.setupEventListeners();
        
        // 定时刷新
        setInterval(() => {
            this.loadStats();
            this.loadTasks();
        }, 10000); // 10秒刷新一次
    }

    setupEventListeners() {
        // 页码筛选
        document.getElementById('pageFilter').addEventListener('change', (e) => {
            this.currentPage = 0;
            this.loadData();
        });

        // 页面大小筛选
        document.getElementById('pageSize').addEventListener('change', (e) => {
            this.pageSize = parseInt(e.target.value);
            this.currentPage = 0;
            this.loadData();
        });
    }

    // 加载统计信息
    async loadStats() {
        try {
            const [taskStats, dataStats] = await Promise.all([
                fetch('/api/crawler/tasks/stats').then(r => r.json()),
                fetch('/api/crawler/data/stats').then(r => r.json())
            ]);

            // 更新任务统计
            document.getElementById('totalTasks').textContent = 
                (taskStats.PENDING || 0) + (taskStats.RUNNING || 0) + (taskStats.COMPLETED || 0) + (taskStats.FAILED || 0);
            document.getElementById('runningTasks').textContent = taskStats.runningCount || 0;
            document.getElementById('pendingTasks').textContent = (taskStats.PENDING || 0) + (taskStats.PAUSED || 0);
            document.getElementById('totalData').textContent = dataStats.totalCount || 0;

        } catch (error) {
            console.error('加载统计信息失败:', error);
        }
    }

    // 加载任务列表
    async loadTasks() {
        try {
            const response = await fetch('/api/crawler/tasks');
            const tasks = await response.json();
            this.renderTasks(tasks);
        } catch (error) {
            console.error('加载任务列表失败:', error);
        }
    }

    // 渲染任务列表
    renderTasks(tasks) {
        const container = document.getElementById('tasksList');
        
        if (tasks.length === 0) {
            container.innerHTML = '<div class="text-center text-muted py-4">暂无任务</div>';
            return;
        }

        const html = tasks.map(task => this.createTaskCard(task)).join('');
        container.innerHTML = html;
    }

    // 创建任务卡片
    createTaskCard(task) {
        const statusClass = this.getStatusClass(task.status);
        const statusText = this.getStatusText(task.status);
        const canStart = ['PENDING', 'PAUSED', 'FAILED'].includes(task.status);
        const canStop = task.status === 'RUNNING';
        const canDelete = !['RUNNING'].includes(task.status);

        return `
            <div class="card task-card mb-3">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-8">
                            <h6 class="card-title mb-1">${task.taskName}</h6>
                            <p class="card-text text-muted small mb-2">${task.description || '无描述'}</p>
                            <div class="row text-muted small">
                                <div class="col-md-4">
                                    <i class="bi bi-link-45deg"></i> ${this.truncateUrl(task.url)}
                                </div>
                                <div class="col-md-4">
                                    <i class="bi bi-file-earmark-text"></i> 最大${task.maxPages}页
                                </div>
                                <div class="col-md-4">
                                    <i class="bi bi-clock"></i> ${this.formatTime(task.createdTime)}
                                </div>
                            </div>
                            ${task.status === 'RUNNING' ? `
                                <div class="progress mt-2" style="height: 6px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated" 
                                         style="width: ${this.calculateProgress(task)}%"></div>
                                </div>
                                <small class="text-muted">第${task.currentPage}页，第${task.currentItemIndex}项</small>
                            ` : ''}
                        </div>
                        <div class="col-md-4 text-end">
                            <span class="badge ${statusClass} status-badge mb-2">${statusText}</span>
                            <div class="btn-group-vertical w-100">
                                ${canStart ? `<button class="btn btn-sm btn-success" onclick="crawlerManager.startTask(${task.id})">
                                    <i class="bi bi-play-fill"></i> 启动
                                </button>` : ''}
                                ${canStop ? `<button class="btn btn-sm btn-warning" onclick="crawlerManager.stopTask(${task.id})">
                                    <i class="bi bi-pause-fill"></i> 停止
                                </button>` : ''}
                                ${canDelete ? `<button class="btn btn-sm btn-danger" onclick="crawlerManager.deleteTask(${task.id})">
                                    <i class="bi bi-trash"></i> 删除
                                </button>` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    // 加载数据列表
    async loadData() {
        try {
            const pageFilter = document.getElementById('pageFilter').value;
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: 'id',
                sortDir: 'desc'
            });

            if (pageFilter) {
                params.append('pageNumber', pageFilter);
            }

            console.log('请求参数:', params.toString());
            const response = await fetch(`/api/crawler/data?${params}`);
            const data = await response.json();
            console.log('API返回数据:', data);
            
            this.renderData(data);
            this.renderPagination(data);
        } catch (error) {
            console.error('加载数据失败:', error);
        }
    }

    // 渲染数据表格
    renderData(data) {
        const tbody = document.getElementById('dataTableBody');
        
        if (data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无数据</td></tr>';
            return;
        }

        const html = data.content.map(item => `
            <tr>
                <td>${item.id}</td>
                <td>${this.escapeHtml(item.companyName || '')}</td>
                <td>${this.escapeHtml(item.productTitle || '')}</td>
                <td>${this.escapeHtml(item.contactPerson || '')}</td>
                <td>${this.escapeHtml(item.landlinePhone || '')}</td>
                <td>${this.escapeHtml(item.mobilePhone || '')}</td>
                <td>${this.escapeHtml(item.address || '')}</td>
                <td>${this.escapeHtml(item.fax || '')}</td>
                <td><span class="badge bg-info">第${item.pageNumber}页</span></td>
                <td>${this.formatTime(item.crawlTime)}</td>
                <td class="url-column" title="${this.escapeHtml(item.sourceUrl || '')}">${this.escapeHtml(item.sourceUrl || '')}</td>
            </tr>
        `).join('');

        tbody.innerHTML = html;
    }

    // 渲染分页
    renderPagination(data) {
        const pagination = document.getElementById('pagination');
        const totalPages = data.totalPages;
        
        // 添加调试信息
        console.log('分页数据:', {
            totalPages: totalPages,
            currentPage: this.currentPage,
            totalElements: data.totalElements,
            size: data.size,
            first: data.first,
            last: data.last
        });
        
        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';
        
        // 上一页
        if (data.first) {
            html += '<li class="page-item disabled"><span class="page-link">上一页</span></li>';
        } else {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(${this.currentPage - 1})">上一页</a></li>`;
        }

        // 页码 - 智能分页显示
        html = this.renderPageNumbers(html, totalPages, this.currentPage);

        // 下一页
        if (data.last) {
            html += '<li class="page-item disabled"><span class="page-link">下一页</span></li>';
        } else {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(${this.currentPage + 1})">下一页</a></li>`;
        }

        pagination.innerHTML = html;
    }

    // 智能渲染页码
    renderPageNumbers(html, totalPages, currentPage) {
        console.log('渲染页码:', { totalPages, currentPage, html });
        
        // 如果总页数小于等于7，显示所有页码
        if (totalPages <= 7) {
            for (let i = 0; i < totalPages; i++) {
                if (i === currentPage) {
                    html += `<li class="page-item active"><span class="page-link">${i + 1}</span></li>`;
                } else {
                    html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(${i})">${i + 1}</a></li>`;
                }
            }
            console.log('总页数<=7，显示所有页码，结果:', html);
            return html;
        }

        // 总页数大于7时，使用智能省略显示
        const showPages = 5; // 显示的页码数量
        let startPage = Math.max(0, currentPage - Math.floor(showPages / 2));
        let endPage = Math.min(totalPages - 1, startPage + showPages - 1);

        // 调整起始页，确保显示足够的页码
        if (endPage - startPage + 1 < showPages) {
            startPage = Math.max(0, endPage - showPages + 1);
        }
        
        console.log('智能分页计算:', { showPages, startPage, endPage });

        // 显示第一页
        if (startPage > 0) {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(0)">1</a></li>`;
            if (startPage > 1) {
                html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            }
        }

        // 显示中间页码
        for (let i = startPage; i <= endPage; i++) {
            if (i === currentPage) {
                html += `<li class="page-item active"><span class="page-link">${i + 1}</span></li>`;
            } else {
                html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(${i})">${i + 1}</a></li>`;
            }
        }

        // 显示最后一页
        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            }
            html += `<li class="page-item"><a class="page-link" href="#" onclick="crawlerManager.goToPage(${totalPages - 1})">${totalPages}</a></li>`;
        }
        
        console.log('智能分页最终结果:', html);
        return html;
    }

    // 搜索数据
    async searchData() {
        const keyword = document.getElementById('searchInput').value.trim();
        if (!keyword) {
            this.loadData();
            return;
        }

        try {
            const response = await fetch(`/api/crawler/data/search?keyword=${encodeURIComponent(keyword)}`);
            const results = await response.json();
            this.renderSearchResults(results);
        } catch (error) {
            console.error('搜索失败:', error);
        }
    }

    // 渲染搜索结果
    renderSearchResults(results) {
        const tbody = document.getElementById('dataTableBody');
        
        if (results.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">未找到匹配结果</td></tr>';
            return;
        }

        const html = results.map(item => `
            <tr>
                <td>${item.id}</td>
                <td>${this.escapeHtml(item.companyName || '')}</td>
                <td>${this.escapeHtml(item.productTitle || '')}</td>
                <td>${this.escapeHtml(item.contactPerson || '')}</td>
                <td>${this.escapeHtml(item.landlinePhone || '')}</td>
                <td>${this.escapeHtml(item.mobilePhone || '')}</td>
                <td>${this.escapeHtml(item.address || '')}</td>
                <td>${this.escapeHtml(item.fax || '')}</td>
                <td><span class="badge bg-info">第${item.pageNumber}页</span></td>
                <td>${this.formatTime(item.crawlTime)}</td>
                <td class="url-column" title="${this.escapeHtml(item.sourceUrl || '')}">${this.escapeHtml(item.sourceUrl || '')}</td>
            </tr>
        `).join('');

        tbody.innerHTML = html;
        document.getElementById('pagination').innerHTML = '';
    }

    // 显示创建任务模态框
    showCreateTaskModal() {
        const modal = new bootstrap.Modal(document.getElementById('createTaskModal'));
        modal.show();
    }

    // 创建任务
    async createTask() {
        const taskName = document.getElementById('taskName').value.trim();
        const url = document.getElementById('taskUrl').value.trim();
        const maxPages = parseInt(document.getElementById('maxPages').value);
        const description = document.getElementById('taskDescription').value.trim();

        if (!taskName || !url || !maxPages) {
            alert('请填写必填字段');
            return;
        }

        try {
            const response = await fetch('/api/crawler/tasks', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    taskName,
                    url,
                    maxPages,
                    description
                })
            });

            if (response.ok) {
                const task = await response.json();
                alert('任务创建成功！');
                
                // 关闭模态框
                bootstrap.Modal.getInstance(document.getElementById('createTaskModal')).hide();
                
                // 清空表单
                document.getElementById('createTaskForm').reset();
                
                // 刷新任务列表
                this.loadTasks();
                this.loadStats();
            } else {
                const error = await response.json();
                alert('创建失败: ' + error.error);
            }
        } catch (error) {
            console.error('创建任务失败:', error);
            alert('创建任务失败');
        }
    }

    // 启动任务
    async startTask(taskId) {
        try {
            const response = await fetch(`/api/crawler/tasks/${taskId}/start`, {
                method: 'POST'
            });

            if (response.ok) {
                alert('任务启动成功！');
                this.loadTasks();
                this.loadStats();
            } else {
                const error = await response.json();
                alert('启动失败: ' + error.error);
            }
        } catch (error) {
            console.error('启动任务失败:', error);
            alert('启动任务失败');
        }
    }

    // 停止任务
    async stopTask(taskId) {
        try {
            const response = await fetch(`/api/crawler/tasks/${taskId}/stop`, {
                method: 'POST'
            });

            if (response.ok) {
                alert('任务停止成功！');
                this.loadTasks();
                this.loadStats();
            } else {
                const error = await response.json();
                alert('停止失败: ' + error.error);
            }
        } catch (error) {
            console.error('停止任务失败:', error);
            alert('停止任务失败');
        }
    }

    // 删除任务
    async deleteTask(taskId) {
        if (!confirm('确定要删除这个任务吗？此操作不可恢复。')) {
            return;
        }

        try {
            const response = await fetch(`/api/crawler/tasks/${taskId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                alert('任务删除成功！');
                this.loadTasks();
                this.loadStats();
            } else {
                const error = await response.json();
                alert('删除失败: ' + error.error);
            }
        } catch (error) {
            console.error('删除任务失败:', error);
            alert('删除任务失败');
        }
    }

    // 跳转到指定页
    goToPage(page) {
        this.currentPage = page;
        this.loadData();
    }

    // 刷新数据
    refreshData() {
        // 重置到第一页
        this.currentPage = 0;
        // 重新加载数据
        this.loadData();
        // 显示刷新提示
        this.showRefreshToast();
    }

    // 显示刷新提示
    showRefreshToast() {
        // 创建一个简单的提示
        const toast = document.createElement('div');
        toast.className = 'alert alert-success alert-dismissible fade show position-fixed';
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 200px;';
        toast.innerHTML = `
            <i class="bi bi-check-circle"></i> 数据已刷新
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(toast);
        
        // 3秒后自动移除
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 3000);
    }

    // 导出当前页数据
    async exportCurrentPage() {
        try {
            this.showExportToast('正在导出当前页数据...', 'info');
            
            const pageFilter = document.getElementById('pageFilter').value;
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: 'id',
                sortDir: 'desc',
                export: 'true'
            });

            if (pageFilter) {
                params.append('pageNumber', pageFilter);
            }

            const response = await fetch(`/api/crawler/data/export?${params}`);
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `1688供应商数据_第${this.currentPage + 1}页_${new Date().toISOString().slice(0, 10)}.xlsx`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
                
                this.showExportToast('当前页数据导出成功！', 'success');
            } else {
                throw new Error('导出失败');
            }
        } catch (error) {
            console.error('导出当前页失败:', error);
            this.showExportToast('导出失败，请重试', 'danger');
        }
    }

    // 导出全部数据
    async exportAllData() {
        try {
            this.showExportToast('正在导出全部数据，请稍候...', 'info');
            
            const pageFilter = document.getElementById('pageFilter').value;
            const params = new URLSearchParams({
                export: 'true',
                all: 'true'
            });

            if (pageFilter) {
                params.append('pageNumber', pageFilter);
            }

            const response = await fetch(`/api/crawler/data/export?${params}`);
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `1688供应商数据_全部_${new Date().toISOString().slice(0, 10)}.xlsx`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
                
                this.showExportToast('全部数据导出成功！', 'success');
            } else {
                throw new Error('导出失败');
            }
        } catch (error) {
            console.error('导出全部数据失败:', error);
            this.showExportToast('导出失败，请重试', 'danger');
        }
    }

    // 显示导出提示
    showExportToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 250px;';
        
        let icon = '';
        switch (type) {
            case 'success':
                icon = 'bi-check-circle';
                break;
            case 'danger':
                icon = 'bi-exclamation-triangle';
                break;
            case 'info':
            default:
                icon = 'bi-info-circle';
                break;
        }
        
        toast.innerHTML = `
            <i class="bi ${icon}"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(toast);
        
        // 5秒后自动移除
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 5000);
    }
    
    // 手动检查失败任务
    async checkFailedTasks() {
        try {
            this.showExportToast('正在检查失败任务...', 'info');
            
            const response = await fetch('/api/crawler/tasks/check-failed', {
                method: 'POST'
            });
            
            if (response.ok) {
                const result = await response.json();
                this.showExportToast('失败任务检查已触发: ' + result.message, 'success');
                
                // 刷新任务列表和统计信息
                setTimeout(() => {
                    this.loadTasks();
                    this.loadStats();
                }, 2000);
            } else {
                const error = await response.json();
                this.showExportToast('检查失败任务失败: ' + error.error, 'danger');
            }
        } catch (error) {
            console.error('检查失败任务失败:', error);
            this.showExportToast('检查失败任务失败，请重试', 'danger');
        }
    }

    // 工具方法
    getStatusClass(status) {
        const statusMap = {
            'PENDING': 'bg-secondary',
            'RUNNING': 'bg-primary',
            'PAUSED': 'bg-warning',
            'COMPLETED': 'bg-success',
            'FAILED': 'bg-danger'
        };
        return statusMap[status] || 'bg-secondary';
    }

    getStatusText(status) {
        const statusMap = {
            'PENDING': '待处理',
            'RUNNING': '运行中',
            'PAUSED': '已暂停',
            'COMPLETED': '已完成',
            'FAILED': '失败'
        };
        return statusMap[status] || status;
    }

    calculateProgress(task) {
        if (task.maxPages <= 0) return 0;
        return Math.min(100, (task.currentPage / task.maxPages) * 100);
    }

    truncateUrl(url) {
        if (url.length > 50) {
            return url.substring(0, 50) + '...';
        }
        return url;
    }

    formatTime(timeStr) {
        if (!timeStr) return '-';
        const date = new Date(timeStr);
        return date.toLocaleString('zh-CN');
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 全局函数
function showCreateTaskModal() {
    crawlerManager.showCreateTaskModal();
}

function createTask() {
    crawlerManager.createTask();
}

function searchData() {
    crawlerManager.searchData();
}

function refreshData() {
    crawlerManager.refreshData();
}

function exportCurrentPage() {
    crawlerManager.exportCurrentPage();
}

function exportAllData() {
    crawlerManager.exportAllData();
}

function checkFailedTasks() {
    crawlerManager.checkFailedTasks();
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    window.crawlerManager = new CrawlerManager();
});
