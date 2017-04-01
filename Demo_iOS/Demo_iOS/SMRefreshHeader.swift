//
//  SMRefreshHeader.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit

public let SMRefreshHeaderHeight: CGFloat = 54

class SMRefreshHeader: SMRefreshComponent {
    var lastUpdatedTimeKey: String = "last_updated_time_key"
    /** 上一次下拉刷新成功的时间 */
    var lastUpdatedTime: Date? {
        set{
            if let newValue = newValue {
                Foundation.UserDefaults.standard.set(newValue, forKey: self.lastUpdatedTimeKey)
                Foundation.UserDefaults.standard.synchronize()
            }
        }
        get{
            return Foundation.UserDefaults.standard.object(forKey: self.lastUpdatedTimeKey) as? Date
        }
    }
    /** 忽略多少scrollView的contentInset的top */
    var ignoreScrollViewContentInsetTop: CGFloat = 0
    
    fileprivate var insetTDelta: CGFloat = 0
    
    
    override func prepare() {
        super.prepare()
        self.sm_height = SMRefreshHeaderHeight
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        self.sm_y = -self.sm_height - ignoreScrollViewContentInsetTop
        
    }
    
    override func scrollViewContentOffsetDidChange(_ change: [NSKeyValueChangeKey : Any]?) {
        super.scrollViewContentOffsetDidChange(change)
        if self.state == .refreshing {
            guard let _ = self.window, let scrollView = self.scrollView else {
                return
            }
            //如果在刷新过程中发生滚动
            var insetTop = -scrollView.sm_offsetY > self.scrollViewOriginInset.top ? -scrollView.sm_offsetY :
                scrollViewOriginInset.top
            insetTop = insetTop > self.sm_height + scrollViewOriginInset.top ? self.sm_height + scrollViewOriginInset.top : insetTop
            scrollView.sm_insetTop = insetTop
            self.insetTDelta = scrollViewOriginInset.top - insetTop
            return
            
        }
        
        guard let scrollView = self.scrollView else {
            return
        }
        
        scrollViewOriginInset = scrollView.contentInset
        
        let offsetY = scrollView.sm_offsetY
        let happenOffsetY = -self.scrollViewOriginInset.top
        // 如果是向上滚动到看不见头部控件，直接返回
        if(offsetY > happenOffsetY) {
            return
        }
        
        let fullVisisableOffset = happenOffsetY - self.sm_height
        let pullingPercent = (happenOffsetY - offsetY) / self.sm_height
        
        
        if scrollView.isDragging {
            // 拖拽中
            self.pullingPercent = pullingPercent
            if self.state == .idle {
                if offsetY < fullVisisableOffset {
                    self.state = .overPulling
                } else if offsetY >= fullVisisableOffset && offsetY < happenOffsetY {
                    self.state = .pulling
                }
                
            } else if self.state == .pulling {
                if offsetY < fullVisisableOffset {
                    self.state = .overPulling
                } else if offsetY >= happenOffsetY {
                    self.state = .idle
                }
            } else if self.state == .overPulling {
                if offsetY >= happenOffsetY {
                    self.state = .idle
                } else if offsetY > fullVisisableOffset {
                    self.state = .pulling
                }
            }
        } else if self.state == .overPulling {
            // 开始刷新
            self.beginRefresh()
        } else if pullingPercent < 1 {
            self.pullingPercent = pullingPercent
        } else if pullingPercent <= 0 {
            self.state = .idle
        }
    }
    
    override func notifyStateChange(_ oldState:RefreshState, newState:RefreshState) {
        if newState == .idle || newState == .pulling {
            if(oldState == .refreshing){
                self.lastUpdatedTime = Date()
                UIView.animate(withDuration: 0.4, animations: {
                    self.scrollView?.sm_insetTop += self.insetTDelta
                }, completion: { finished in
                    self.pullingPercent = 0.0
                })
            }
            
        } else if newState == .overPulling {
            
        } else if newState == .refreshing {
            DispatchQueue.main.async(execute: {
                UIView.animate(withDuration: 0.4, animations: {
                    let top = self.scrollViewOriginInset.top + self.sm_height
                    self.scrollView?.sm_insetTop = top
                    self.scrollView?.setContentOffset(CGPoint(x: 0, y: -top), animated: false)
                }, completion: { finished in
                    self.executeRefreshingCallback()
                })
            })
        }
    }
}
