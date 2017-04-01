//
//  ResuableView.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit

protocol ReusableView: class {}

extension ReusableView where Self: UIView {
    
    static var reuseIdentifier: String {
        return String(describing: self)
    }
}

extension UITableViewCell: ReusableView { }
