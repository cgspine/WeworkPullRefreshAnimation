//
//  ViewController.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    lazy var tableView:UITableView = {
        let tableView = UITableView()
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(UITableViewCell.self)
        let refreashHeader = WWRefreshView()
        refreashHeader.refreshingCallback = {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + .seconds(3), execute: {[weak self] in
                self?.tableView.sm_header?.endRefresh()
            })
        }
        tableView.sm_header = refreashHeader
        return tableView
    }()
    
    override func loadView() {
        super.loadView()
        view.backgroundColor = UIColor.white
        self.view.addSubview(tableView)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        tableView.frame = self.view.bounds
        tableView.sm_y = 20
        if let header = tableView.sm_header{
            // TODO why header.sm_width == 0?
            //header.sm_width = SMRefreshHeaderHeight
            header.sm_x = (tableView.sm_width - SMRefreshHeaderHeight)/2
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

}

extension ViewController: UITableViewDelegate{
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat{
        return 48
    }
}

extension ViewController: UITableViewDataSource{
    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int{
        return 20
    }
    
    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell{
        let cell = tableView.dequeueReusableCell(forIndexPath: indexPath)
        cell.textLabel?.text = "item \(indexPath.row)"
        return cell
    }
}
