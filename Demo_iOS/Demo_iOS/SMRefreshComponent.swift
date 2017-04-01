//
//  SMRefreshComponent.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit

enum RefreshState {
    case idle           // 默认状态
    case pulling        //
    case overPulling    // 松开就可刷新的状态
    case refreshing     // 刷新中状态
    case willRefresh    // 即将刷新的状态(这是为了防止view还没有显示出来就调用了BeginRefresh,判断方法是self.window == nil)
    case noMoreData     // 数据加载完状态
}

class SMRefreshComponent: UIView {
    var state:RefreshState = .idle {
        willSet{
            if newValue != state {
                self.notifyStateChange(state, newState: newValue)
            }
        }
    }
    
    fileprivate var panGestureRecognizer: UIPanGestureRecognizer?
    
    var scrollView: UIScrollView?
    
    var scrollViewOriginInset: UIEdgeInsets = UIEdgeInsets.zero
    
    var refreshingCallback: (() -> Void)?
    
    var pullingPercent:CGFloat = 0
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        prepare()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    
    override func willMove(toSuperview newSuperview: UIView?) {
        if let newSuperview = newSuperview {
            // 确保其父类是UIScrollView
            if !newSuperview.isKind(of: UIScrollView.self) {
                return
            }
            // 旧的父控件移除KVO
            self.removeObservers()
            
            self.sm_width = newSuperview.sm_width
            self.sm_x = 0
            self.scrollView  = newSuperview as? UIScrollView
            if let scrollView = self.scrollView {
                scrollView.alwaysBounceVertical = true
                self.scrollViewOriginInset = scrollView.contentInset
            }
            
            self.addObservers()
        }
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        
        if self.state == .willRefresh {
            self.state = .refreshing
        }
    }
    
    
    func prepare() {
        self.backgroundColor = UIColor.clear
    }
    
    func notifyStateChange(_ oldState:RefreshState, newState:RefreshState) {
        DispatchQueue.main.async(execute: {
            self.setNeedsLayout()
        })
    }
    
    
    // MARK: KVO监听
    
    fileprivate func addObservers(){
        let options:NSKeyValueObservingOptions = [.new, .old]
        self.scrollView?.addObserver(self, forKeyPath: "contentOffset", options: options, context: nil)
        self.scrollView?.addObserver(self, forKeyPath: "contentSize", options: options, context: nil)
        self.panGestureRecognizer = self.scrollView?.panGestureRecognizer
        self.panGestureRecognizer?.addObserver(self, forKeyPath: "state", options: options, context: nil)
    }
    
    fileprivate func removeObservers(){
        self.superview?.removeObserver(self, forKeyPath: "contentOffset")
        self.superview?.removeObserver(self, forKeyPath: "contentSize")
        self.panGestureRecognizer?.removeObserver(self, forKeyPath: "state")
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if !self.isUserInteractionEnabled {
            return
        }
        
        
        if let keyPath = keyPath {
            // 这个就算看不见也需要处理
            if keyPath == "contentOffset" {
                self.scrollViewContentOffsetDidChange(change)
            }
            
            if self.isHidden {
                return
            }
            
            if keyPath == "contentSize" {
                self.scrollViewContentSizeDidChange(change)
            } else if keyPath == "state" {
                self.scrollViewPanStateDidChange(change)
            }
        }
    }
    
    func scrollViewContentOffsetDidChange(_ change: [NSKeyValueChangeKey : Any]?) {}
    func scrollViewContentSizeDidChange(_ change: [NSKeyValueChangeKey : Any]?) {}
    func scrollViewPanStateDidChange(_ change: [NSKeyValueChangeKey : Any]?) {}
    
    // MARK: 刷新状态控制
    
    func beginRefresh() {
        self.pullingPercent = 1.0
        
        if self.window != nil {
            self.state = .refreshing
        } else {
            if self.state != .refreshing {
                self.state = .willRefresh
                self.setNeedsLayout()
            }
        }
    }
    
    func endRefresh(){
        DispatchQueue.main.async(execute: {
            self.state = .idle
        })
    }
    
    func isRefreshing() -> Bool{
        return state == .refreshing || state == .willRefresh
    }
    
    // MARK: 执行回调
    func executeRefreshingCallback() {
        DispatchQueue.main.async(execute: {
            if let refreshingCallback = self.refreshingCallback {
                refreshingCallback()
            }
        })
    }
}
