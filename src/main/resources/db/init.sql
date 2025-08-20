-- 创建爬取进度表
CREATE TABLE IF NOT EXISTS crawl_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url TEXT NOT NULL,
    current_page INT NOT NULL DEFAULT 1,
    current_item_index INT NOT NULL DEFAULT 0,
    total_pages INT NOT NULL DEFAULT 5,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
    last_crawled_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_url (url(255)),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建爬取任务表
CREATE TABLE IF NOT EXISTS crawl_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    url TEXT NOT NULL,
    max_pages INT NOT NULL DEFAULT 5,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_page INT NOT NULL DEFAULT 1,
    current_item_index INT NOT NULL DEFAULT 0,
    total_items_crawled INT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_time DATETIME NULL,
    completed_time DATETIME NULL,
    description TEXT,
    UNIQUE KEY uk_task_name (task_name),
    INDEX idx_status (status),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建供应商信息表
CREATE TABLE IF NOT EXISTS manufacturer_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(500),
    product_title VARCHAR(1000),
    price VARCHAR(100),
    contact_person VARCHAR(200),
    phone_number VARCHAR(100),
    address TEXT,
    fax VARCHAR(100),
    main_products TEXT,
    contact_info TEXT,
    source_url TEXT,
    page_number INT,
    crawl_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_company (company_name(100)),
    INDEX idx_page (page_number),
    INDEX idx_crawl_time (crawl_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入第一页已完成的爬取进度数据
-- 这里假设你已经爬取了第一页，现在从第二页开始
INSERT INTO crawl_progress (
    url, 
    current_page, 
    current_item_index, 
    total_pages, 
    status, 
    last_crawled_time
) VALUES (
    'https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AA%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AA%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true',
    2,  -- 当前页设置为第2页
    0,  -- 商品索引从0开始
    5,  -- 总页数5页
    'IN_PROGRESS',  -- 状态为进行中
    NOW()  -- 最后爬取时间
);

-- 插入示例爬取任务
INSERT INTO crawl_tasks (
    task_name,
    url,
    max_pages,
    status,
    description
) VALUES (
    '合肥锂电池组批发商爬取',
    'https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AA%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AA%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true',
    5,
    'COMPLETED',
    '合肥锂电池组批发商和制造商信息爬取任务'
);

-- 显示插入结果
SELECT '数据库初始化完成！' AS message;
SELECT '爬取进度已设置为第2页开始' AS progress_status;
SELECT '爬取任务表已创建并包含示例任务' AS task_status;
SELECT * FROM crawl_progress;
SELECT * FROM crawl_tasks;
