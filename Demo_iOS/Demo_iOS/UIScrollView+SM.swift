//
//  UIScrollView+SM.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit

extension UIScrollView {
    fileprivate struct AssociatedKey {
        static var sm_refresh_header_key = "sm_refresh_header_key"
    }
    
    var sm_header: SMRefreshHeader? {
        get{
            return objc_getAssociatedObject(self, &AssociatedKey.sm_refresh_header_key) as? SMRefreshHeader
        }
        set{
            if let newValue = newValue {
                if self.sm_header == nil || self.sm_header! != newValue {
                    self.sm_header?.removeFromSuperview()
                    self.insertSubview(newValue, at: 0)
                    
                    self.willChangeValue(forKey: "sm_header")
                    objc_setAssociatedObject(self, &AssociatedKey.sm_refresh_header_key, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                    self.didChangeValue(forKey: "sm_header")
                }
            }
            
        }
    }
    
    var sm_insetTop: CGFloat {
        set{
            var inset = self.contentInset
            inset.top = newValue
            self.contentInset = inset
        }
        get{
            return self.contentInset.top
        }
    }
    
    var sm_insetLeft: CGFloat {
        set{
            var inset = self.contentInset
            inset.left = newValue
            self.contentInset = inset
        }
        get{
            return self.contentInset.left
        }
    }
    
    var sm_insetBottom: CGFloat {
        set{
            var inset = self.contentInset
            inset.bottom = newValue
            self.contentInset = inset
        }
        get{
            return self.contentInset.bottom
        }
    }
    
    var sm_insetRight: CGFloat {
        set{
            var inset = self.contentInset
            inset.right = newValue
            self.contentInset = inset
        }
        get{
            return self.contentInset.right
        }
    }
    
    var sm_offsetX: CGFloat {
        set{
            var offset = self.contentOffset
            offset.x = newValue
            self.contentOffset = offset
        }
        get{
            return self.contentOffset.x
        }
    }
    
    var sm_offsetY: CGFloat {
        set{
            var offset = self.contentOffset
            offset.y = newValue
            self.contentOffset = offset
        }
        get{
            return self.contentOffset.y
        }
    }
    
    var sm_contentWidth: CGFloat {
        set{
            var content = self.contentSize
            content.width = newValue
            self.contentSize = content
        }
        get{
            return self.contentSize.width
        }
    }
    
    var sm_contentHeight: CGFloat {
        set{
            var content = self.contentSize
            content.height = newValue
            self.contentSize = content
        }
        get{
            return self.contentSize.height
        }
    }
}
