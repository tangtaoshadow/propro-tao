//== Class definition

var DatatableResponsiveColumnsDemo = function () {
    //== Private functions

    // basic demo
    var demo = function () {

        var datatable = $('.m_datatable').mDatatable({
            // datasource definition
            data: {
                type: 'remote',
                source: {
                    read: {
                        url: 'https://keenthemes.com/metronic/preview/inc/api/datatables/demos/default.php'
                    }
                },
                pageSize: 10,
                serverPaging: true,
                serverFiltering: true,
                serverSorting: true
            },

            // layout definition
            layout: {
                theme: 'default', // datatable theme
                class: '', // custom wrapper class
                scroll: false, // enable/disable datatable scroll both horizontal and vertical when needed.
                footer: false // display/hide footer
            },

            // column sorting
            sortable: true,

            pagination: true,

            search: {
                input: $('#generalSearch')
            },

            // columns definition
            columns: [{
                field: "RecordID",
                title: "#",
                sortable: false, // disable sort for this column
                width: 40,
                textAlign: 'center',
                selector: {class: 'm-checkbox--solid m-checkbox--brand'}
            }, {
                field: "OrderID",
                title: "Order ID",
                filterable: false, // disable or enable filtering
                width: 150
            }, {
                field: "ShipCity",
                title: "Ship City",
                responsive: {visible: 'lg'}
            }, {
                field: "Website",
                title: "Website",
                width: 200,
                responsive: {visible: 'lg'}
            }, {
                field: "Department",
                title: "Department",
                responsive: {visible: 'lg'}
            }, {
                field: "ShipDate",
                title: "Ship Date",
                responsive: {visible: 'lg'}
            }, {
                field: "Actions",
                width: 110,
                title: "Actions",
                sortable: false,
                overflow: 'visible',
                template: function (row, index, datatable) {
                    var dropup = (datatable.getPageSize() - index) <= 4 ? 'dropup' : '';
                    return '\
						<div class="dropdown ' + dropup + '">\
							<a href="#" class="btn m-btn m-btn--hover-accent m-btn--icon m-btn--icon-only m-btn--pill" data-toggle="dropdown">\
                                <i class="la la-ellipsis-h"></i>\
                            </a>\
						  	<div class="dropdown-menu dropdown-menu-right">\
						    	<a class="dropdown-item" href="#"><i class="la la-edit"></i> Edit Details</a>\
						    	<a class="dropdown-item" href="#"><i class="la la-leaf"></i> Update Status</a>\
						    	<a class="dropdown-item" href="#"><i class="la la-print"></i> Generate Report</a>\
						  	</div>\
						</div>\
						<a href="#" class="m-portlet__nav-link btn m-btn m-btn--hover-accent m-btn--icon m-btn--icon-only m-btn--pill" title="Edit details">\
							<i class="la la-edit"></i>\
						</a>\
						<a href="#" class="m-portlet__nav-link btn m-btn m-btn--hover-danger m-btn--icon m-btn--icon-only m-btn--pill" title="Delete">\
							<i class="la la-trash"></i>\
						</a>\
					';
                }
            }]
        });
    };

    return {
        // public functions
        init: function () {
            demo();
        }
    };
}();

var LibraryTable = function (remoteUrl) {
    var table = function (remoteUrl) {

        var datatable = $('.m-datatable').mDatatable({
            data: {
                type: 'remote',
                source: {
                    read: {
                        url: remoteUrl
                    }
                },
                pageSize: 10,
                serverPaging: true,
                serverFiltering: true,
                serverSorting: true
            },
            search: {
                input: $('#generalSearch')
            },
            columns: [
                {
                    field: 'id',
                    title: 'Id'
                },
                {
                    field: 'name',
                    title: '库名'
                },
                {
                    field: 'instrument',
                    title: '设备名称'
                },
                {
                    field: 'proteinCount',
                    title: '蛋白质数目'
                },
                {
                    field: 'peptideCount',
                    title: '肽段数目'
                },
                {
                    field: 'transitionCount',
                    title: 'Transition数目'
                },
                {
                    field: 'description',
                    title: '详细描述'
                },
                {
                    field: 'createDate',
                    title: '创建时间'
                },
                {
                    field: 'lastModifiedDate',
                    title: '最后修改时间'
                },
                {
                    field: "Actions",
                    width: 110,
                    title: "操作",
                    sortable: false,
                    overflow: 'visible',
                    template: function (row, index, datatable) {
                        return '\
						<a href="/library/edit/' + row.id + '" class="m-portlet__nav-link btn m-btn m-btn--hover-accent m-btn--icon m-btn--icon-only m-btn--pill" title="编辑">\
							<i class="la la-edit"></i>\
						</a>\
						<a href="/library/detail/' + row.id + '" class="m-portlet__nav-link btn m-btn m-btn--hover-accent m-btn--icon m-btn--icon-only m-btn--pill" title="查看详情">\
                            <i class="la la-navicon"></i>\
                        </a>\
					';
                    }
                }
            ]
        });
    };

    return {
        init: function (remoteUrl) {
            table(remoteUrl);
        }
    };
}();